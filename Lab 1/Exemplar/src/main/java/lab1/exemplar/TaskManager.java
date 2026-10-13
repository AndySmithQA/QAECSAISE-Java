package lab1.exemplar;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Typical AI-generated task manager for Lab 1 review.
 * The quality issues marked below are intentional.
 */
public class TaskManager {
    public final Map<String, TaskRecord> tasks = new LinkedHashMap<>();
    public final Map<String, Object> resources = new LinkedHashMap<>();

    public void addTask(String taskId, String name, List<String> dependencies, int duration, List<String> resourcesNeeded) {
        // ISSUE: No input validation
        TaskRecord task = new TaskRecord();
        task.name = name;
        task.dependencies = dependencies;
        task.duration = duration;
        task.resources = resourcesNeeded;
        task.startDate = null;
        tasks.put(taskId, task);
    }

    public boolean detectCircularDependencies() {
        // ISSUE: Inefficient O(n²) algorithm, doesn't handle all cases
        for (String taskId : tasks.keySet()) {
            List<String> visited = new ArrayList<>();
            if (hasCycle(taskId, visited)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasCycle(String taskId, List<String> visited) {
        // ISSUE: Incorrect cycle detection logic
        if (visited.contains(taskId)) {
            return true;
        }
        visited.add(taskId);
        for (String dependency : tasks.get(taskId).dependencies) {
            if (hasCycle(dependency, visited)) {
                return true;
            }
        }
        return false;
    }

    public int calculateEarliestStart(String taskId) {
        // ISSUE: No memoization, exponential time complexity
        if (tasks.get(taskId).dependencies.isEmpty()) {
            return 0;
        }
        int maxEnd = 0;
        for (String dependency : tasks.get(taskId).dependencies) {
            int dependencyStart = calculateEarliestStart(dependency);
            int dependencyEnd = dependencyStart + tasks.get(dependency).duration;
            maxEnd = Math.max(maxEnd, dependencyEnd);
        }
        return maxEnd;
    }

    public static class TaskRecord {
        public String name;
        public List<String> dependencies = new ArrayList<>();
        public int duration;
        public List<String> resources;
        public LocalDateTime startDate;
    }
}
