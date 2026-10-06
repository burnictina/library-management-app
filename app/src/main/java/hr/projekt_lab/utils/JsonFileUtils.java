package hr.projekt_lab.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class JsonFileUtils {

    private static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.INDENT_OUTPUT);


    public static <T> List<T> readListFromFile(File file, TypeReference<List<T>> typeRef) {
        try {
            if (!file.exists()) return new ArrayList<>();
            return mapper.readValue(file, typeRef);
        } catch (Exception e) {
            throw new RuntimeException("Error while reading JSON: " + file.getName(), e);
        }
    }

    public static <T> void writeListToFile(File file, List<T> list) {
        try {
            mapper.writeValue(file, list);
        } catch (Exception e) {
            throw new RuntimeException("Error while writing JSON: " + file.getName(), e);
        }
    }
}

