package nz.co.solnet.api_test;

import au.com.origin.snapshots.Expect;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import nz.co.solnet.Main;
import nz.co.solnet.api.tasks.GsonLocalDateAdapter;
import nz.co.solnet.database.DBTestUtils;
import org.apache.logging.log4j.Logger;
import org.junit.*;

import static org.apache.logging.log4j.LogManager.getLogger;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.time.LocalDate;

import au.com.origin.snapshots.junit4.SnapshotRunner;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import static nz.co.solnet.api_test.TestCase.Operation.HttpVerb.*;

@RunWith(SnapshotRunner.class)
public class ApiTests {

    private static String BASE_URL;

    private static final Logger logger = getLogger(ApiTests.class);

    private Gson gson = new GsonBuilder().registerTypeAdapter(LocalDate.class, new GsonLocalDateAdapter()).create();
    private static Expect expect; // Injected by the SnapshotRunner

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void beforeAll() {

        System.setProperty("task.api.database.url", "jdbc:derby:memory:testdb");
        System.setProperty("task.api.port", "8090");
        System.setProperty("task.api.database.username", "test");
        System.setProperty("task.api.database.password", "test");
        System.setProperty("task.api.shutdown.secret", "secret");

        BASE_URL = "http://localhost:" + System.getProperty("task.api.port");

        try {
            Main.main(new String[]{});
        } catch (Exception e) {
            e.printStackTrace();
        }
        logger.info("--------------------------------------------------------");
        logger.info("---------------- TASK API TESTS STARTED ----------------");
        logger.info("--------------------------------------------------------");
    }

    @Before
    public void setUp() {
        DBTestUtils.cleanDatabase();
        expect = expect.comparator(new IgnoreCapableComparator(new String[]{"id", "creation_date"}));
        logger.info(name.getMethodName());
    }

    private IgnoreCapableComparator comparator = new IgnoreCapableComparator(new String[]{"id", "creation_date"});

    @Test
    public void create_201() throws IOException {
        // Given
        TestCase create201 = new TestCase(BASE_URL);
        String[] chainedParameters = new String[]{"id"};
        create201.addOperation(POST, "/api/tasks", "{ \"title\" : \"Test name\", \"description\" : \"Test description\", \"status\" : \"TODO\", \"due_date\" : \"2023-04-04\" }", chainedParameters);

        // When
        TestCase.Results results = create201.execute();

        // Then
        assertEquals(201, results.getLast().getStatusCode());
        expect.toMatchSnapshot(results.getLast().getJson());
    }

    @Test
    public void create_201_invalidStatus() throws IOException {
        // Given
        TestCase create201 = new TestCase(BASE_URL);
        create201.addOperation(POST, "/api/tasks", "{ \"title\" : \"Test name\", \"description\" : \"Test description\", \"status\" : \"INVALID\", \"due_date\" : \"2023-04-04\" }");

        // When
        TestCase.Results results = create201.execute();

        // Then
        assertEquals(201, results.getLast().getStatusCode());
        expect.toMatchSnapshot(results.getLast().getJson());
    }

    @Test
    public void create_400_string_validations() throws IOException {
        // Given
        TestCase create400 = new TestCase(BASE_URL);

        String tooLongString = getLongString(2000);
        create400.addOperation(POST, "/api/tasks", "{ \"title\" : \"\", \"description\" : \"" + tooLongString + "\", \"status\" : \"TODO\", \"due_date\" : \"2023-04-04\" }");

        // When
        TestCase.Results results = create400.execute();

        // Then
        assertEquals(400, results.getLast().getStatusCode());
        expect.toMatchSnapshot(results.getLast().getJson());
    }

    @Test
    public void get_200() throws IOException {
        // Given
        TestCase get200 = new TestCase(BASE_URL);
        String[] chainedParameters = new String[]{"id"};
        get200.addOperation(POST, "/api/tasks", "{ \"title\" : \"Test name\", \"description\" : \"Test description\", \"status\" : \"TODO\", \"due_date\" : \"2023-04-04\" }", chainedParameters);
        get200.addOperation(GET, "/api/tasks/{id}");

        // When
        TestCase.Results results = get200.execute();

        // Then
        assertEquals(200, results.getLast().getStatusCode());
        expect.toMatchSnapshot(results.getLast().getJson());
    }

    @Test
    public void get_404() throws IOException {
        // Given
        TestCase get404 = new TestCase(BASE_URL);
        get404.addOperation(GET, "/api/tasks/1");

        // When
        TestCase.Results results = get404.execute();

        // Then
        assertEquals(404, results.getLast().getStatusCode());
        expect.toMatchSnapshot(results.getLast().getJson());
    }

    @Test
    public void getCollection_200() throws IOException {
        // Given
        TestCase getCollection200 = new TestCase(BASE_URL);

        for (int i = 0; i < 10; i++) {
            getCollection200.addOperation(POST, "/api/tasks", "{ \"title\" : \"Test name\", \"description\" : \"Test description\", \"status\" : \"TODO\", \"due_date\" : \"2023-04-04\" }");
        }
        String[] chainedParameters = new String[]{"id"};
        getCollection200.addOperation(POST, "/api/tasks", "{ \"title\" : \"Test name\", \"description\" : \"Test description\", \"status\" : \"TODO\", \"due_date\" : \"2023-04-04\" }", chainedParameters);
        getCollection200.addOperation(GET, "/api/tasks");

        // When
        TestCase.Results results = getCollection200.execute();

        // Then
        assertEquals(200, results.getLast().getStatusCode());
        expect.toMatchSnapshot(results.getLast().getJson());
    }

    @Test
    public void getCollection_400_invalidParameters() throws IOException {
        // Given
        TestCase getCollection200_invalidParameters = new TestCase(BASE_URL);
        getCollection200_invalidParameters.addOperation(POST, "/api/tasks", "{ \"title\" : \"Test name\", \"description\" : \"Test description\", \"status\" : \"TODO\", \"due_date\" : \"2023-04-04\" }");
        getCollection200_invalidParameters.addOperation(GET, "/api/tasks?startDate=INVALID&endDate=INVALID&status=INVALID");

        // When
        TestCase.Results results = getCollection200_invalidParameters.execute();

        // Then
        assertEquals(400, results.getLast().getStatusCode());
        expect.toMatchSnapshot(results.getLast().getJson());
    }

    @Test
    public void getCollection_200_reversedDates() throws IOException {
        // Given
        TestCase getCollection200_reversedDates = new TestCase(BASE_URL);

        for (int i = 0; i < 10; i++) {
            getCollection200_reversedDates.addOperation(POST, "/api/tasks", "{ \"title\" : \"Test name\", \"description\" : \"Test description\", \"status\" : \"TODO\", \"due_date\" : \"2023-04-04\" }");
        }
        String[] chainedParameters = new String[]{"id"};
        getCollection200_reversedDates.addOperation(POST, "/api/tasks", "{ \"title\" : \"Test name\", \"description\" : \"Test description\", \"status\" : \"TODO\", \"due_date\" : \"2023-04-04\" }", chainedParameters);
        getCollection200_reversedDates.addOperation(GET, "/api/tasks?startDate=2023-04-05&endDate=2023-04-01");

        // When
        TestCase.Results results = getCollection200_reversedDates.execute();

        // Then
        assertEquals(200, results.getLast().getStatusCode());
        expect.toMatchSnapshot(results.getLast().getJson());
    }

    @Test
    public void getCollection_200_retrieveOneStatus() throws IOException {
        // Given
        TestCase getCollection_200_retrieveOneStatus = new TestCase(BASE_URL);
        getCollection_200_retrieveOneStatus.addOperation(POST, "/api/tasks", "{ \"title\" : \"Test name\", \"description\" : \"Test description\", \"status\" : \"TODO\", \"due_date\" : \"2023-04-04\" }");
        getCollection_200_retrieveOneStatus.addOperation(POST, "/api/tasks", "{ \"title\" : \"Test name\", \"description\" : \"Test description\", \"status\" : \"TODO\", \"due_date\" : \"2023-04-04\" }");
        getCollection_200_retrieveOneStatus.addOperation(POST, "/api/tasks", "{ \"title\" : \"Test name\", \"description\" : \"Test description\", \"status\" : \"IN_PROGRESS\", \"due_date\" : \"2023-04-04\" }");
        getCollection_200_retrieveOneStatus.addOperation(POST, "/api/tasks", "{ \"title\" : \"Test name\", \"description\" : \"Test description\", \"status\" : \"IN_PROGRESS\", \"due_date\" : \"2023-04-04\" }");
        getCollection_200_retrieveOneStatus.addOperation(POST, "/api/tasks", "{ \"title\" : \"Test name\", \"description\" : \"Test description\", \"status\" : \"DONE\", \"due_date\" : \"2023-04-04\" }");
        getCollection_200_retrieveOneStatus.addOperation(POST, "/api/tasks", "{ \"title\" : \"Test name\", \"description\" : \"Test description\", \"status\" : \"DONE\", \"due_date\" : \"2023-04-04\" }");
        getCollection_200_retrieveOneStatus.addOperation(GET, "/api/tasks?status=TODO");

        // When
        TestCase.Results results = getCollection_200_retrieveOneStatus.execute();

        // Then
        assertEquals(200, results.getLast().getStatusCode());
        expect.toMatchSnapshot(results.getLast().getJson());
    }

    @Test
    public void getCollection_200_retrieveMultipleStatuses() throws IOException {
        // Given
        TestCase getCollection_200_retrieveMultipleStatuses = new TestCase(BASE_URL);
        getCollection_200_retrieveMultipleStatuses.addOperation(POST, "/api/tasks", "{ \"title\" : \"Test name\", \"description\" : \"Test description\", \"status\" : \"TODO\", \"due_date\" : \"2023-04-04\" }");
        getCollection_200_retrieveMultipleStatuses.addOperation(POST, "/api/tasks", "{ \"title\" : \"Test name\", \"description\" : \"Test description\", \"status\" : \"TODO\", \"due_date\" : \"2023-04-04\" }");
        getCollection_200_retrieveMultipleStatuses.addOperation(POST, "/api/tasks", "{ \"title\" : \"Test name\", \"description\" : \"Test description\", \"status\" : \"IN_PROGRESS\", \"due_date\" : \"2023-04-04\" }");
        getCollection_200_retrieveMultipleStatuses.addOperation(POST, "/api/tasks", "{ \"title\" : \"Test name\", \"description\" : \"Test description\", \"status\" : \"IN_PROGRESS\", \"due_date\" : \"2023-04-04\" }");
        getCollection_200_retrieveMultipleStatuses.addOperation(POST, "/api/tasks", "{ \"title\" : \"Test name\", \"description\" : \"Test description\", \"status\" : \"DONE\", \"due_date\" : \"2023-04-04\" }");
        getCollection_200_retrieveMultipleStatuses.addOperation(POST, "/api/tasks", "{ \"title\" : \"Test name\", \"description\" : \"Test description\", \"status\" : \"DONE\", \"due_date\" : \"2023-04-04\" }");
        getCollection_200_retrieveMultipleStatuses.addOperation(GET, "/api/tasks?status=TODO,IN_PROGRESS");

        // When
        TestCase.Results results = getCollection_200_retrieveMultipleStatuses.execute();

        // Then
        assertEquals(200, results.getLast().getStatusCode());
        expect.toMatchSnapshot(results.getLast().getJson());
    }

    @Test
    public void getCollection_400_malformedMultipleStatus() throws IOException {
        // Given
        TestCase getCollection_400_malformedMultipleStatus = new TestCase(BASE_URL);
        getCollection_400_malformedMultipleStatus.addOperation(POST, "/api/tasks", "{ \"title\" : \"Test name\", \"description\" : \"Test description\", \"status\" : \"TODO\", \"due_date\" : \"2023-04-04\" }");
        getCollection_400_malformedMultipleStatus.addOperation(POST, "/api/tasks", "{ \"title\" : \"Test name\", \"description\" : \"Test description\", \"status\" : \"TODO\", \"due_date\" : \"2023-04-04\" }");
        getCollection_400_malformedMultipleStatus.addOperation(POST, "/api/tasks", "{ \"title\" : \"Test name\", \"description\" : \"Test description\", \"status\" : \"IN_PROGRESS\", \"due_date\" : \"2023-04-04\" }");
        getCollection_400_malformedMultipleStatus.addOperation(POST, "/api/tasks", "{ \"title\" : \"Test name\", \"description\" : \"Test description\", \"status\" : \"IN_PROGRESS\", \"due_date\" : \"2023-04-04\" }");

        getCollection_400_malformedMultipleStatus.addOperation(GET, "/api/tasks?status=TODO,IN_PROGRESS,INVALID_STATUS");

        // When
        TestCase.Results results = getCollection_400_malformedMultipleStatus.execute();

        // Then
        assertEquals(400, results.getLast().getStatusCode());
        expect.toMatchSnapshot(results.getLast().getJson());
    }

    @Test
    public void get_400_malformedId() throws IOException {
        // Given
        TestCase get400 = new TestCase(BASE_URL);
        get400.addOperation(GET, "/api/tasks/abc");

        // When
        TestCase.Results results = get400.execute();

        // Then
        assertEquals(400, results.getLast().getStatusCode());
        expect.toMatchSnapshot(results.getLast().getJson());
    }

    @Test
    public void update_200() throws IOException {
        // Given
        TestCase update200 = new TestCase(BASE_URL);
        String[] chainedParameters = new String[]{"id"};
        update200.addOperation(POST, "/api/tasks", "{ \"title\" : \"Test name\", \"description\" : \"Test description\", \"status\" : \"TODO\", \"due_date\" : \"2023-04-04\" }", chainedParameters);
        update200.addOperation(PUT, "/api/tasks/{id}", "{ \"title\" : \"Updated test name\", \"description\" : \"Updated Test description\", \"status\" : \"DONE\", \"due_date\" : \"2023-05-04\" }");

        // When
        TestCase.Results results = update200.execute();

        // Then
        assertEquals(200, results.getLast().getStatusCode());
        expect.toMatchSnapshot(results.getLast().getJson());
    }

    @Test
    public void update_404() throws IOException {
        // Given
        TestCase update404 = new TestCase(BASE_URL);
        update404.addOperation(PUT, "/api/tasks/1", "{ \"title\" : \"Updated test name\", \"description\" : \"Updated Test description\", \"status\" : \"DONE\", \"due_date\" : \"2023-05-04\" }");

        // When
        TestCase.Results results = update404.execute();

        // Then
        assertEquals(404, results.getLast().getStatusCode());
        expect.toMatchSnapshot(results.getLast().getJson());
    }

    @Test
    public void update_400_malformedId() throws IOException {
        // Given
        TestCase update400_malformedId = new TestCase(BASE_URL);
        update400_malformedId.addOperation(PUT, "/api/tasks/abc", "{ \"title\" : \"Updated test name\", \"description\" : \"Updated Test description\", \"status\" : \"DONE\", \"due_date\" : \"2023-05-04\" }");

        // When
        TestCase.Results results = update400_malformedId.execute();

        // Then
        assertEquals(400, results.getLast().getStatusCode());
        expect.toMatchSnapshot(results.getLast().getJson());
    }

    @Test
    public void delete_204() throws IOException {
        // Given
        TestCase delete204 = new TestCase(BASE_URL);
        String[] chainedParameters = new String[]{"id"};
        delete204.addOperation(POST, "/api/tasks", "{ \"title\" : \"Test name\", \"description\" : \"Test description\", \"status\" : \"TODO\", \"due_date\" : \"2023-04-04\" }", chainedParameters);
        delete204.addOperation(DELETE, "/api/tasks/{id}");

        // When
        TestCase.Results results = delete204.execute();

        // Then
        assertEquals(204, results.getLast().getStatusCode());
        expect.toMatchSnapshot(results.getLast().getJson());
    }

    @Test
    public void delete_400_malformedId() throws IOException {
        // Given
        TestCase delete400_malformedId = new TestCase(BASE_URL);
        delete400_malformedId.addOperation(DELETE, "/api/tasks/abc");

        // When
        TestCase.Results results = delete400_malformedId.execute();

        // Then
        assertEquals(400, results.getLast().getStatusCode());
        expect.toMatchSnapshot(results.getLast().getJson());
    }

    @Test
    public void delete_404() throws IOException {
        // Given
        TestCase delete404 = new TestCase(BASE_URL);
        delete404.addOperation(DELETE, "/api/tasks/1");

        // When
        TestCase.Results results = delete404.execute();

        // Then
        assertEquals(404, results.getLast().getStatusCode());
        expect.toMatchSnapshot(results.getLast().getJson());
    }

    private String getLongString(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append("a");
        }
        return sb.toString();
    }

}
