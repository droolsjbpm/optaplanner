/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
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

package org.optaplanner.examples.conferencescheduling.swingui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;

import org.optaplanner.examples.common.swingui.SolutionPanel;
import org.optaplanner.examples.conferencescheduling.domain.ConferenceSolution;
import org.optaplanner.examples.conferencescheduling.persistence.ConferenceSchedulingCfpDevoxxImporter;
import org.optaplanner.examples.conferencescheduling.persistence.ConferenceSchedulingXlsxFileIO;
import org.optaplanner.persistence.common.api.domain.solution.SolutionFileIO;

public class ConferenceSchedulingPanel extends SolutionPanel<ConferenceSolution> {

    public static final String LOGO_PATH = "/org/optaplanner/examples/conferencescheduling/swingui/conferenceSchedulingLogo.png";

    public ConferenceSchedulingPanel() {
        JButton publishButton = new JButton("Publish");
        publishButton.addActionListener(actionEvent -> {
            solutionBusiness.getSolution().getTalkList().forEach(talk -> {
                talk.setPublishedTimeslot(talk.getTimeslot());
                talk.setPublishedRoom(talk.getRoom());
            });
        });

        JButton showInLibreOfficeOrExcelButton = new JButton("Show in LibreOffice or Excel");
        showInLibreOfficeOrExcelButton.addActionListener(event -> {
            SolutionFileIO<ConferenceSolution> solutionFileIO = new ConferenceSchedulingXlsxFileIO();
            File tempFile;
            try {
                tempFile = File.createTempFile(solutionBusiness.getSolutionFileName(), "." + solutionFileIO.getOutputFileExtension());
            } catch (IOException e) {
                throw new IllegalStateException("Failed to create temp file.", e);
            }
            solutionFileIO.write(solutionBusiness.getSolution(), tempFile);
            try {
                Desktop.getDesktop().open(tempFile);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to show temp file (" + tempFile + ") in LibreOffice or Excel.", e);
            }
        });

        JPanel showPanel = new JPanel();
        JPanel importPanel = new JPanel();
        showPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        importPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

        showPanel.add(showInLibreOfficeOrExcelButton);
        showPanel.add(new JLabel("Changes to that file are ignored unless you explicitly save it there and open it here."));
        importPanel.add(publishButton);

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new GridLayout(2, 1));
        buttonsPanel.add(showPanel);
        buttonsPanel.add(importPanel);

        setLayout(new BorderLayout());
        add(buttonsPanel, BorderLayout.NORTH);
    }

    @Override
    public void resetPanel(ConferenceSolution solution) {
    }


}
