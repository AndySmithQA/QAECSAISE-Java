package lab1.improved;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reference solution for Lab 1: validated input, correct cycle detection, and a memoized schedule.
 */
public class TaskManager {
    private static final int WHITE = 0;
    private static final int GRAY = 1;
    private static final int BLACK = 2;

    public final Map<String, Task> tasks = new LinkedHashMap<>();
    public final Map<String, Integer> resources = new LinkedHashMap<>();
    private final Map<String, Integer> earliestStartCache = new LinkedHashMap<>();

    /** Add a task with proper validation. */
    public void addTask(String taskId, String name, List<String> dependencies, int duration, List<String> resourcesNeeded) {
        if (taskId == null || taskId.isEmpty()) {
            throw new TaskManagerException("Task ID must be a non-empty string");
        }
        if (tasks.containsKey(taskId)) {
            throw new TaskManagerException("Task " + taskId + " already exists");
        }
        if (duration <= 0) {
            throw new TaskManagerException("Duration must be positive");
        }
        if (name == null || name.isEmpty()) {
            throw new TaskManagerException("Task name must be a non-empty string");
        }

        for (String dependency : dependencies) {
            if (!tasks.containsKey(dependency) && !dependency.equals(taskId)) {
                logWarning("Dependency " + dependency + " for task " + taskId + " not found");
            }
        }

        tasks.put(taskId, new Task(taskId, name, dependencies, duration, resourcesNeeded));
        earliestStartCache.clear();
        logInfo("Added task " + taskId + " with " + dependencies.size() + " dependencies");
    }

    /** Cycle detection using depth-first search with coloring. */
    public boolean detectCircularDependencies() {
        Map<String, Integer> colors = new LinkedHashMap<>();
        for (String taskId : tasks.keySet()) {
            colors.put(taskId, WHITE);
        }

        for (String taskId : tasks.keySet()) {
            if (colors.get(taskId) == WHITE && hasCycle(taskId, colors)) {
                return true;
            }
        }
        return false;
    }

    /** Calculate earliest start time with memoization. */
    public int calculateEarliestStart(String taskId) {
        if (!tasks.containsKey(taskId)) {
            throw new TaskManagerException("Task " + taskId + " not found");
        }
        if (earliestStartCache.containsKey(taskId)) {
            return earliestStartCache.get(taskId);
        }

        Task task = tasks.get(taskId);
        if (task.dependencies.isEmpty()) {
            earliestStartCache.put(taskId, 0);
            return 0;
        }

        int maxEnd = 0;
        for (String dependency : task.dependencies) {
            if (tasks.containsKey(dependency)) {
                int dependencyStart = calculateEarliestStart(dependency);
                int dependencyEnd = dependencyStart + tasks.get(dependency).duration;
                maxEnd = Math.max(maxEnd, dependencyEnd);
            }
        }

        earliestStartCache.put(taskId, maxEnd);
        return maxEnd;
    }

    /** Find the longest path through the project. */
    public List<String> getCriticalPath() {
        if (detectCircularDependencies()) {
            throw new TaskManagerException("Cannot calculate critical path: circular dependencies detected");
        }

        Map<String, List<String>> successors = new LinkedHashMap<>();
        for (String taskId : tasks.keySet()) {
            successors.put(taskId, new ArrayList<>());
        }
        for (Map.Entry<String, Task> entry : tasks.entrySet()) {
            for (String dependency : entry.getValue().dependencies) {
                if (successors.containsKey(dependency)) {
                    successors.get(dependency).add(entry.getKey());
                }
            }
        }

        List<String> endTasks = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : successors.entrySet()) {
            if (entry.getValue().isEmpty()) {
                endTasks.add(entry.getKey());
            }
        }
        if (endTasks.isEmpty()) {
            return new ArrayList<>();
        }

        String latestEndTask = endTasks.get(0);
        int latestFinish = calculateEarliestStart(latestEndTask) + tasks.get(latestEndTask).duration;
        for (int index = 1; index < endTasks.size(); index++) {
            int finish = calculateEarliestStart(endTasks.get(index)) + tasks.get(endTasks.get(index)).duration;
            if (finish > latestFinish) {
                latestFinish = finish;
                latestEndTask = endTasks.get(index);
            }
        }
        return findPath(latestEndTask);
    }

    private boolean hasCycle(String taskId, Map<String, Integer> colors) {
        if (colors.get(taskId) == GRAY) {
            return true;
        }
        if (colors.get(taskId) == BLACK) {
            return false;
        }

        colors.put(taskId, GRAY);
        for (String dependency : tasks.get(taskId).dependencies) {
            if (tasks.containsKey(dependency) && hasCycle(dependency, colors)) {
                return true;
            }
        }
        colors.put(taskId, BLACK);
        return false;
    }

    private List<String> findPath(String taskId) {
        Task task = tasks.get(taskId);
        if (task.dependencies.isEmpty()) {
            List<String> path = new ArrayList<>();
            path.add(taskId);
            return path;
        }

        int taskStart = calculateEarliestStart(taskId);
        String criticalDependency = null;
        for (String dependency : task.dependencies) {
            if (tasks.containsKey(dependency)) {
                int dependencyEnd = calculateEarliestStart(dependency) + tasks.get(dependency).duration;
                if (dependencyEnd == taskStart) {
                    criticalDependency = dependency;
                    break;
                }
            }
        }

        if (criticalDependency != null) {
            List<String> path = findPath(criticalDependency);
            path.add(taskId);
            return path;
        }

        List<String> path = new ArrayList<>();
        path.add(taskId);
        return path;
    }

    private static void logInfo(String message) {
        System.err.println("INFO: " + message);
    }

    private static void logWarning(String message) {
        System.err.println("WARNING: " + message);
    }
}
