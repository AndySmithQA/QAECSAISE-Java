package lab1.improved;

import java.time.LocalDateTime;
import java.util.List;

public class Task {
    public final String taskId;
    public final String name;
    public final List<String> dependencies;
    public final int duration;
    public final List<String> resourcesNeeded;
    public LocalDateTime startDate;
    public Integer earliestStart;

    public Task(String taskId, String name, List<String> dependencies, int duration, List<String> resourcesNeeded) {
        this.taskId = taskId;
        this.name = name;
        this.dependencies = dependencies;
        this.duration = duration;
        this.resourcesNeeded = resourcesNeeded;
    }
}
