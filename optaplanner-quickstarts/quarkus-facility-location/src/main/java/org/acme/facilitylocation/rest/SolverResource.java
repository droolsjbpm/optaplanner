package org.acme.facilitylocation.rest;

import java.util.Optional;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.acme.facilitylocation.domain.FacilityLocationProblem;
import org.acme.facilitylocation.persistence.FacilityLocationProblemRepository;
import org.optaplanner.core.api.score.ScoreManager;
import org.optaplanner.core.api.solver.SolverManager;

@Path("/flp")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SolverResource {

    private static final long PROBLEM_ID = 0L;

    private final FacilityLocationProblemRepository repository;
    private final SolverManager<FacilityLocationProblem, Long> solverManager;
    private final ScoreManager<FacilityLocationProblem> scoreManager;

    public SolverResource(
            FacilityLocationProblemRepository repository,
            SolverManager<FacilityLocationProblem, Long> solverManager,
            ScoreManager<FacilityLocationProblem> scoreManager) {
        this.repository = repository;
        this.solverManager = solverManager;
        this.scoreManager = scoreManager;
    }

    private Status statusFromSolution(FacilityLocationProblem solution) {
        return new Status(
                solution,
                scoreManager.explainScore(solution).getSummary(),
                solverManager.getSolverStatus(PROBLEM_ID));
    }

    @GET
    @Path("status")
    public Status status() {
        return statusFromSolution(repository.solution().orElse(FacilityLocationProblem.empty()));
    }

    @POST
    @Path("solve")
    public void solve() {
        Optional<FacilityLocationProblem> maybeSolution = repository.solution();
        maybeSolution.ifPresent(facilityLocationProblem -> solverManager.solveAndListen(
                PROBLEM_ID,
                id -> facilityLocationProblem,
                repository::update));
    }

    @POST
    @Path("stopSolving")
    public void stopSolving() {
        solverManager.terminateEarly(PROBLEM_ID);
    }
}
