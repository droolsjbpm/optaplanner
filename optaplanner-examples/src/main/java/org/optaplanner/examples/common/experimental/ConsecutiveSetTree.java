/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.optaplanner.examples.common.experimental;

import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A ConsecutiveSetTree determine what value are consecutive. A sequence x1,x2,x3,...,xn
 * is understood to be consecutive by d iff x2 - x1 <= d, x3 -x2 <= d, ..., xn - x(n-1) <= d.
 * This datastructure can be thought as an interval tree that maps the point p to
 * the interval [p, p + d].
 *
 * @param <ValueType_> The type of value stored (examples: shifts)
 * @param <PointType_> The type of the point (examples: int, LocalDateTime)
 * @param <DifferenceType_> The type of the difference (examples: int, Duration)
 */
public class ConsecutiveSetTree<ValueType_, PointType_ extends Comparable<PointType_>, DifferenceType_ extends Comparable<DifferenceType_>> {
    private final Function<ValueType_, PointType_> indexFunction;
    private final BiFunction<PointType_, PointType_, DifferenceType_> differenceFunction;
    private final DifferenceType_ maxDifference;
    private final DifferenceType_ zeroDifference;
    private final Map<ValueType_, Integer> itemToCountMap;
    private final NavigableSet<ValueType_> itemSet;
    private final NavigableMap<ValueType_, Sequence<ValueType_>> startItemToSequence;
    private final NavigableMap<ValueType_, Break<ValueType_, DifferenceType_>> startItemToPreviousBreak;
    private final Comparator<ValueType_> comparator;

    private final MapValuesIterable<ValueType_, Sequence<ValueType_>> sequenceList;
    private final MapValuesIterable<ValueType_, Break<ValueType_, DifferenceType_>> breakList;

    private final ConsecutiveData<ValueType_, DifferenceType_> consecutiveData;

    public ConsecutiveSetTree(Function<ValueType_, PointType_> indexFunction,
            BiFunction<PointType_, PointType_, DifferenceType_> differenceFunction, DifferenceType_ maxDifference,
            DifferenceType_ zeroDifference) {
        this.indexFunction = indexFunction;
        this.differenceFunction = differenceFunction;
        this.maxDifference = maxDifference;
        this.zeroDifference = zeroDifference;
        // Identity Hashcode for duplicate protection
        // Ex: two different games on the same time slot
        comparator = Comparator.comparing(indexFunction).thenComparing(System::identityHashCode);
        itemToCountMap = new IdentityHashMap<>();
        itemSet = new TreeSet<>(comparator);
        startItemToSequence = new TreeMap<>(comparator);
        startItemToPreviousBreak = new TreeMap<>(comparator);
        consecutiveData = new ConsecutiveData<>(this);
        sequenceList = new MapValuesIterable<>(startItemToSequence);
        breakList = new MapValuesIterable<>(startItemToPreviousBreak);
    }

    protected NavigableSet<ValueType_> getItemSet() {
        return itemSet;
    }

    public Comparator<ValueType_> getComparator() {
        return comparator;
    }

    public Iterable<Sequence<ValueType_>> getConsecutiveSequences() {
        return sequenceList;
    }

    public Iterable<Break<ValueType_, DifferenceType_>> getBreaks() {
        return breakList;
    }

    public void updateLengthOfBreak(Break<ValueType_, DifferenceType_> theBreak) {
        theBreak.setLength(getBreakLengthBetween(theBreak.getAfterItem(), theBreak.getBeforeItem()));
    }

    public DifferenceType_ getBreakLengthBetween(ValueType_ from, ValueType_ to) {
        return differenceFunction.apply(indexFunction.apply(from), indexFunction.apply(to));
    }

    public ConsecutiveData<ValueType_, DifferenceType_> getConsecutiveData() {
        return consecutiveData;
    }

    public ValueType_ getEndItem(ValueType_ key) {
        return startItemToSequence.get(key).getItems().last();
    }

    private boolean isFirstSuccessorOfSecond(PointType_ first, ValueType_ firstValue, PointType_ second,
            ValueType_ secondValue) {
        DifferenceType_ difference = differenceFunction.apply(second, first);
        int differenceComparedTo0 = difference.compareTo(zeroDifference);
        if (differenceComparedTo0 > 0) {
            return difference.compareTo(maxDifference) <= 0;
        } else {
            return differenceComparedTo0 == 0 && System.identityHashCode(secondValue) < System.identityHashCode(firstValue);
        }
    }

    private void addBetweenItems(ValueType_ item, PointType_ itemIndex,
            ValueType_ firstBeforeItem, ValueType_ endOfBeforeSequenceItem, PointType_ endOfBeforeSequenceItemIndex,
            ValueType_ firstAfterItem, PointType_ startOfAfterSequenceIndex) {
        if (isFirstSuccessorOfSecond(itemIndex, item, endOfBeforeSequenceItemIndex, endOfBeforeSequenceItem)) {
            // We need to extend the first bag
            Sequence<ValueType_> prevBag = startItemToSequence.get(firstBeforeItem);
            if (isFirstSuccessorOfSecond(startOfAfterSequenceIndex, firstAfterItem, itemIndex, item)) {
                // We need to merge the two bags
                startItemToPreviousBreak.remove(firstAfterItem);
                Sequence<ValueType_> afterBag = startItemToSequence.remove(firstAfterItem);
                prevBag.merge(afterBag);
            } else {
                prevBag.addAfterEnd(item);
                Break<ValueType_, DifferenceType_> nextBreak = startItemToPreviousBreak.get(firstAfterItem);
                nextBreak.setAfterItem(item);
                nextBreak.setLength(differenceFunction.apply(itemIndex, startOfAfterSequenceIndex));
            }
        } else {
            // Don't need to extend the first bag
            if (isFirstSuccessorOfSecond(startOfAfterSequenceIndex, firstAfterItem, itemIndex, item)) {
                // We need to move the after bag to use item as key
                Sequence<ValueType_> afterBag = startItemToSequence.remove(firstAfterItem);
                afterBag.addBeforeStart(item);
                startItemToSequence.put(item, afterBag);
                Break<ValueType_, DifferenceType_> prevBreak = startItemToPreviousBreak.remove(firstAfterItem);
                prevBreak.setBeforeItem(item);
                prevBreak.setLength(differenceFunction.apply(endOfBeforeSequenceItemIndex, itemIndex));
                startItemToPreviousBreak.put(item, prevBreak);
            } else {
                // Start a new bag of consecutive items
                Sequence<ValueType_> newBag = new Sequence<>(this, item);
                startItemToSequence.put(item, newBag);
                Break<ValueType_, DifferenceType_> nextBreak = startItemToPreviousBreak.get(firstAfterItem);
                nextBreak.setAfterItem(item);
                nextBreak.setLength(differenceFunction.apply(itemIndex, startOfAfterSequenceIndex));
                startItemToPreviousBreak.put(item, new Break<>(item, endOfBeforeSequenceItem,
                        differenceFunction.apply(endOfBeforeSequenceItemIndex, itemIndex)));
            }
        }
    }

    public boolean add(ValueType_ item) {
        if (itemToCountMap.containsKey(item)) {
            itemToCountMap.merge(item, 1, Integer::sum);
            return true;
        }
        itemSet.add(item);
        itemToCountMap.put(item, 1);
        ValueType_ firstBeforeItem = startItemToSequence.floorKey(item);
        PointType_ itemIndex = indexFunction.apply(item);
        if (firstBeforeItem != null) {
            ValueType_ endOfBeforeSequenceItem = getEndItem(firstBeforeItem);
            PointType_ endOfBeforeSequenceIndex = indexFunction.apply(endOfBeforeSequenceItem);
            if (itemIndex.compareTo(endOfBeforeSequenceIndex) < 0 || (itemIndex.compareTo(endOfBeforeSequenceIndex) == 0
                    && System.identityHashCode(endOfBeforeSequenceItem) > System.identityHashCode(item))) {
                // Item is already in the bag; do nothing
                return true;
            } else {
                // Item is outside the bag
                ValueType_ firstAfterItem = startItemToSequence.higherKey(item);
                if (firstAfterItem != null) {
                    PointType_ startOfAfterSequenceIndex = indexFunction.apply(firstAfterItem);
                    addBetweenItems(item, itemIndex, firstBeforeItem, endOfBeforeSequenceItem,
                            endOfBeforeSequenceIndex, firstAfterItem, startOfAfterSequenceIndex);
                } else {
                    if (isFirstSuccessorOfSecond(itemIndex, item, endOfBeforeSequenceIndex, endOfBeforeSequenceItem)) {
                        // We need to extend the first bag
                        Sequence<ValueType_> prevBag = startItemToSequence.get(firstBeforeItem);
                        // No break since afterItem is null
                        prevBag.addAfterEnd(item);
                    } else {
                        // Start a new bag of consecutive items
                        Sequence<ValueType_> newBag = new Sequence<>(this, item);
                        startItemToSequence.put(item, newBag);
                        startItemToPreviousBreak.put(item,
                                new Break<>(item, endOfBeforeSequenceItem,
                                        differenceFunction.apply(endOfBeforeSequenceIndex, itemIndex)));
                    }
                }
            }
        } else {
            // No items before it
            ValueType_ firstAfterItem = startItemToSequence.higherKey(item);
            if (firstAfterItem != null) {
                PointType_ startOfAfterSequenceIndex = indexFunction.apply(firstAfterItem);

                if (isFirstSuccessorOfSecond(startOfAfterSequenceIndex, firstAfterItem, itemIndex, item)) {
                    // We need to move the after bag to use item as key
                    Sequence<ValueType_> afterBag = startItemToSequence.remove(firstAfterItem);
                    afterBag.addBeforeStart(item);
                    // No break since this is the first sequence
                    startItemToSequence.put(item, afterBag);
                } else {
                    // Start a new bag of consecutive items
                    Sequence<ValueType_> newBag = new Sequence<>(this, item);
                    startItemToSequence.put(item, newBag);
                    startItemToPreviousBreak.put(firstAfterItem,
                            new Break<>(firstAfterItem, item, differenceFunction.apply(itemIndex, startOfAfterSequenceIndex)));
                }
            } else {
                // Start a new bag of consecutive items
                Sequence<ValueType_> newBag = new Sequence<>(this, item);
                startItemToSequence.put(item, newBag);
                // Bag have no other items, so no break
            }
        }
        return true;
    }

    public boolean remove(ValueType_ item) {
        if (!itemToCountMap.containsKey(item)) {
            // Item not in bag
            return false;
        }
        itemToCountMap.computeIfPresent(item, (key, count) -> (count > 1) ? count - 1 : null);
        if (itemToCountMap.containsKey(item)) {
            // Item still in bag
            return true;
        }

        // Item is removed from bag
        ValueType_ firstBeforeItem = startItemToSequence.floorKey(item);
        Sequence<ValueType_> bag = startItemToSequence.get(firstBeforeItem);
        ValueType_ endItem = bag.getItems().last();
        itemSet.remove(item);

        // Count of item in bag is 0
        if (bag.isEmpty()) {
            startItemToSequence.remove(firstBeforeItem);
            Break<ValueType_, DifferenceType_> removedBreak = startItemToPreviousBreak.remove(firstBeforeItem);
            Map.Entry<ValueType_, Break<ValueType_, DifferenceType_>> extendedBreakEntry =
                    startItemToPreviousBreak.higherEntry(firstBeforeItem);
            if (extendedBreakEntry != null) {
                if (removedBreak != null) {
                    Break<ValueType_, DifferenceType_> extendedBreak = extendedBreakEntry.getValue();
                    extendedBreak.setAfterItem(removedBreak.getAfterItem());
                    updateLengthOfBreak(extendedBreak);
                } else {
                    startItemToPreviousBreak.remove(extendedBreakEntry.getKey());
                }
            }
            return true;
        }

        // Bag is not empty
        return removeItemFromBag(bag, item, firstBeforeItem, endItem);
    }

    private boolean removeItemFromBag(Sequence<ValueType_> bag, ValueType_ item, ValueType_ sequenceStart,
            ValueType_ sequenceEnd) {
        if (item.equals(sequenceStart)) {
            // Change start key to the item after this one
            bag.updateEndpoints();
            startItemToSequence.remove(sequenceStart);
            Break<ValueType_, DifferenceType_> extendedBreak = startItemToPreviousBreak.remove(sequenceStart);
            startItemToSequence.put(bag.getItems().first(), bag);
            if (extendedBreak != null) {
                extendedBreak.setBeforeItem(bag.getItems().first());
                updateLengthOfBreak(extendedBreak);
                startItemToPreviousBreak.put(bag.getItems().first(), extendedBreak);
            }
            return true;
        }
        if (item.equals(sequenceEnd)) {
            // Need to update break information
            bag.updateEndpoints();
            Map.Entry<ValueType_, Break<ValueType_, DifferenceType_>> extendedBreakEntry =
                    startItemToPreviousBreak.higherEntry(item);
            if (extendedBreakEntry != null) {
                Break<ValueType_, DifferenceType_> extendedBreak = extendedBreakEntry.getValue();
                extendedBreak.setAfterItem(bag.getItems().last());
                updateLengthOfBreak(extendedBreak);
            }
            return true;
        }

        ValueType_ firstAfterItem = bag.getItems().higher(item);
        ValueType_ firstBeforeItem = bag.getItems().lower(item);

        if (isFirstSuccessorOfSecond(
                indexFunction.apply(firstAfterItem), firstAfterItem,
                indexFunction.apply(firstBeforeItem), firstBeforeItem)) {
            // Bag is not split since the next two items are still close enough
            return true;
        }

        // Need to split bag into two halves
        // Both halves are not empty as the item was not an endpoint
        // Additional, the breaks before and after the broken sequence
        // are not affected since an endpoint was not removed
        Sequence<ValueType_> splitBag = bag.split(item);
        startItemToSequence.put(splitBag.getItems().first(), splitBag);
        startItemToPreviousBreak.put(splitBag.getItems().first(), new Break<>(
                splitBag.getItems().first(), bag.getItems().last(),
                getBreakLengthBetween(bag.getItems().last(), splitBag.getItems().first())));
        return true;
    }

}
