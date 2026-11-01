package com.example.todolist.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tasks that were just created but are not yet confirmed by the server are shown together with the tasks
 * the server sent. Without this a task created while the connection is down would "disappear" from the list.
 */
public final class TaskMerge {

    public interface IdOf<T> {
        String idOf(T item);
    }

    private TaskMerge() {
    }

    /**
     * @return all server items, then the pending items the server does not know yet (by id), in their order.
     *         The inputs are not modified.
     */
    public static <T> List<T> withPending(List<T> fromServer, List<T> pending, IdOf<T> ids) {
        List<T> result = new ArrayList<>(fromServer);
        if (pending.isEmpty()) return result;
        Set<String> known = new HashSet<>();
        for (T item : fromServer) known.add(ids.idOf(item));
        for (T item : pending) {
            String id = ids.idOf(item);
            if (id != null && known.add(id)) result.add(item);
        }
        return result;
    }
}
