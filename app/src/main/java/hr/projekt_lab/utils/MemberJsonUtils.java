package hr.projekt_lab.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import hr.projekt_lab.entities.Member;

import java.io.File;
import java.util.List;
import java.util.Optional;

public class MemberJsonUtils {
    private static final File MEMBER_FILE = new File("data/members.json");

    private MemberJsonUtils() {}

    public static Optional<List<Member>> ucitajClanove() {
        List<Member> members = JsonFileUtils.readListFromFile(
                MEMBER_FILE,
                new TypeReference<>() {
                }
        );
        return Optional.ofNullable(members);
    }

    public static void spremiClanove(List<Member> members) {
        JsonFileUtils.writeListToFile(MEMBER_FILE, members);
    }
}
