package amazed.solver;

import amazed.maze.Maze;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutionException;

/**
 * <code>ForkJoinSolver</code> implements a solver for
 * <code>Maze</code> objects using a fork/join multi-thread
 * depth-first search.
 * <p>
 * Instances of <code>ForkJoinSolver</code> should be run by a
 * <code>ForkJoinPool</code> object.
 */


public class ForkJoinSolver extends SequentialSolver{

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
     *  @param maze        the maze to be searched
     * @param forkAfter   the number of steps (visited nodes) after
     *                    which a parallel task is forked; if
     *                    <code>forkAfter &lt;= 0</code> the solver never
     */
    //TODO change javadoc
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
     * TODO!: Provide an implementation of parallel depth-first search using fork/join parallelism.
     * You only need to implement method parallelDepthFirstSearch() in class ForkJoinSolver.
     * You are allowed to use auxiliary (private) methods and attributes, but you should not modify other parts of the given implementation.
     * @return
     */
    private List<Integer> parallelDepthFirstSearch(){

        int player = maze.newPlayer(start);
        int counter = 0;

        frontier.push(start);

        while (!frontier.empty()){

            int current = frontier.pop();

            if (maze.hasGoal(current)){
                maze.move(player,current);
                return pathFromTo(start,current);
            }

            if (!visited.contains(current)){

                maze.move(player,current);
                visited.add(current);
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

        List<Integer> l1 = new LinkedList<>();
        for (Map.Entry<Integer, ForkJoinSolver> p: players.entrySet()) {
            List<Integer> l = p.getValue().join();
            if (l != null) {
                List<Integer> l2 = pathFromTo(start, p.getValue().start);
                if (l2 != null) {
                    l1.addAll(l2);
                    l1.addAll(l);
                }
            }
        }
        if (!l1.isEmpty())
            return l1;

/*

        SequentialSolver solution

        // one player active on the maze at start
        int player = maze.newPlayer(start);
        // start with start node
        frontier.push(start);
        // as long as not all nodes have been processed
        while (!frontier.empty()) {
            // get the new node to process
            int current = frontier.pop();
            // if current node has a goal
            if (maze.hasGoal(current)) {
                // move player to goal
                maze.move(player, current);
                // search finished: reconstruct and return path
                return pathFromTo(start, current);
            }
            // if current node has not been visited yet
            if (!visited.contains(current)) {
                // move player to current node
                maze.move(player, current);
                // mark node as visited
                visited.add(current);
                // for every node nb adjacent to current
                for (int nb: maze.neighbors(current)) {
                    // add nb to the nodes to be processed
                    frontier.push(nb);
                    // if nb has not been already visited,
                    // nb can be reached from current (i.e., current is nb's predecessor)
                    if (!visited.contains(nb))
                        predecessor.put(nb, current);
                }
            }
        }
        // all nodes explored, no goal found
        return null;

*/
        return null;
    }
}
