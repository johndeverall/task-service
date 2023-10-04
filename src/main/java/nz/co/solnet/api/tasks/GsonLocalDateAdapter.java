package nz.co.solnet.api.tasks;

import com.google.gson.*;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.apache.logging.log4j.LogManager.getLogger;

/**
 * Used by Gson to convert LocalDate to and from JSON.
 */
public class GsonLocalDateAdapter implements JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {

    private static final Logger logger = getLogger(TaskServlet.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public JsonElement serialize(LocalDate localDate, Type type, JsonSerializationContext jsonSerializationContext) {
        if (localDate == null) {
            return JsonNull.INSTANCE;
        }
        String formattedDate = localDate.format(DATE_FORMATTER);
        return new JsonPrimitive(formattedDate);
    }

    @Override
    public LocalDate deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        LocalDate localDate = null;

        if (jsonElement.isJsonNull()) {
            return null;
        }
        String dateStr = jsonElement.getAsString();

        return LocalDate.parse(dateStr, DATE_FORMATTER);
    }
}