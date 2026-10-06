package hr.projekt_lab.libraryutils;

import java.util.List;
import java.util.Optional;

public final class SelectionUtils {
    private SelectionUtils() {
    }

    public static <T> Optional<T> chooseFromList(List<? extends T> list, int index) {
        return (index >= 0 && index < list.size())
                ? Optional.of(list.get(index))
                : Optional.empty();
    }
}
