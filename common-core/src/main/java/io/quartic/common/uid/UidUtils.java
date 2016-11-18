package io.quartic.common.uid;

import java.util.Collection;
import java.util.List;

import static java.util.stream.Collectors.toList;

public final class UidUtils {
    private UidUtils() {}

    public static String stringify(Uid uid) {
        return uid.uid();
    }

    public static List<String> stringify(Collection<? extends Uid> uids) {
        return uids.stream().map(UidUtils::stringify).collect(toList());
    }
}
