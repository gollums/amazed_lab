package amazed.solver;

import amazed.maze.Maze;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <code>ForkJoinSolver</code> implements a solver for
 * <code>Maze</code> objects using a fork/join multi-thread
 * depth-first search.
 * <p>
 * Instances of <code>ForkJoinSolver</code> should be run by a
 * <code>ForkJoinPool</code> object.
 */


public class ForkJoinSolver extends SequentialSolver{

    private static AtomicBoolean  running = new AtomicBoolean(true);
    private Map<Integer, ForkJoinSolver> players;
    private static Set<Integer> visited = new ConcurrentSkipListSet<>();

    /**
     * Creates a solver that searches in <code>maze</code> from the
     * start node to a goal.
     *
     * @param maze   the maze to be searched
     */
    public ForkJoinSolver(Maze maze)
    {
        super(maze);

    }

    /**
     * Creates a solver that searches in <code>maze</code> from the
     * start node to a goal, forking after a given number of visited
     * nodes.
     *
     * @param maze        the maze to be searched
     * @param forkAfter   the number of steps (visited nodes) after
     *                    which a parallel task is forked; if
     *                    <code>forkAfter &lt;= 0</code> the solver never
     *                    forks new tasks
     */
    public ForkJoinSolver(Maze maze, int forkAfter)
    {
        this(maze);
        this.forkAfter = forkAfter;
        this.players = new HashMap<>();
    }

    /**
     * Creates a solver that searches in <code>maze</code> from the
     * start node to a goal, forking after a given number of visited
     * nodes.
     * @param maze        the maze to be searched
     * @param forkAfter   the number of steps (visited nodes) after
     *                    which a parallel task is forked; if
     *                    <code>forkAfter &lt;= 0</code> the solver never
     * @param start       the position to start from
     */
    public ForkJoinSolver(Maze maze, int forkAfter, int start)
    {
        this(maze, forkAfter);
        this.start = start;

    }

    /**
     * Searches for and returns the path, as a list of node
     * identifiers, that goes from the start node to a goal node in
     * the maze. If such a path cannot be found (because there are no
     * goals, or all goals are unreacheable), the method returns
     * <code>null</code>.
     *
     * @return   the list of node identifiers from the start node to a
     *           goal node in the maze; <code>null</code> if such a path cannot
     *           be found.
     */
    @Override
    public List<Integer> compute()
    {
        return parallelDepthFirstSearch();
    }


    /**
     * Searches the maze and splits the
     * @return List of positions to the goal or null if not found
     */
    private List<Integer> parallelDepthFirstSearch(){

        int player = maze.newPlayer(start);
        int counter = 0;

        frontier.push(start);

        while (!frontier.empty() && running.get()){
            int current = frontier.pop();

            if (maze.hasGoal(current)){
                maze.move(player,current);
                running.set(false);
                return pathFromTo(start,current);
            }

            if (visited.add(current)){
                maze.move(player,current);
                for(int nb: maze.neighbors(current)) {
                    if (counter >= forkAfter - 1 && maze.neighbors(current).size() > 2) {
                        counter = 0;
                        if (!visited.contains(nb)) {
                            players.put(current, (ForkJoinSolver) new ForkJoinSolver(maze, forkAfter, nb).fork());
                        }
                    } else {
                        frontier.push(nb);
                    }
                    if (!visited.contains(nb)) {
                        predecessor.put(nb, current);
                    }
                }
            }
            counter++;
        }

        //Collects all children results
        List<Integer> l1 = new LinkedList<>();
        for (Map.Entry<Integer, ForkJoinSolver> p: players.entrySet()) {
            List<Integer> l = p.getValue().join();
            if (l != null && l1.isEmpty()) {
                List<Integer> l2 = pathFromTo(start, p.getKey());
                if (l2 != null) {
                    l1.addAll(l2);
                    l1.addAll(l);
                }
            }
        }

        // return the path from this start to the childs start if a goal was found
        if (!l1.isEmpty()) {
            return l1;
        }

        return null;
    }
}
