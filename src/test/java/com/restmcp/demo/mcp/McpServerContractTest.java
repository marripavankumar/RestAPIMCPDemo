package com.restmcp.demo.mcp;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class McpServerContractTest {

    static final List<String> EXPECTED_TOOLS = List.of(
            "list_departments", "get_department", "get_department_employees", "get_department_summary",
            "create_department", "update_department", "delete_department", "assign_department_manager",
            "search_employees", "get_employee", "create_employee", "update_employee", "delete_employee",
            "transfer_employee");

    @LocalServerPort
    int port;

    McpSyncClient client;

    @BeforeEach
    void connect() {
        var transport = HttpClientStreamableHttpTransport.builder("http://localhost:" + port)
                .endpoint("/mcp")
                .build();
        client = McpClient.sync(transport).requestTimeout(Duration.ofSeconds(10)).build();
        client.initialize();
    }

    @AfterEach
    void close() {
        client.closeGracefully();
    }

    static String text(McpSchema.CallToolResult result) {
        return ((McpSchema.TextContent) result.content().getFirst()).text();
    }

    McpSchema.CallToolResult call(String tool, Map<String, Object> args) {
        return client.callTool(new McpSchema.CallToolRequest(tool, args));
    }

    McpSchema.CallToolResult ok(String tool, Map<String, Object> args) {
        var result = call(tool, args);
        assertThat(result.isError()).as("%s failed: %s", tool, text(result)).isNotEqualTo(Boolean.TRUE);
        return result;
    }

    McpSchema.CallToolResult error(String tool, Map<String, Object> args) {
        var result = call(tool, args);
        assertThat(result.isError()).as("%s should fail: %s", tool, text(result)).isTrue();
        return result;
    }

    // --- discovery -----------------------------------------------------------------------------------------

    @Test
    void advertisesServerInfo() {
        assertThat(client.getServerInfo().name()).isEqualTo("employee-department-mcp");
        assertThat(client.getServerInfo().version()).isEqualTo("1.0.0");
    }

    @Test
    void advertisesExactlyTheExpectedTools() {
        var tools = client.listTools().tools();

        assertThat(tools).extracting(McpSchema.Tool::name).containsExactlyInAnyOrderElementsOf(EXPECTED_TOOLS);
        assertThat(tools).allSatisfy(t -> assertThat(t.description()).hasSizeGreaterThan(40));
    }

    @Test
    void annotatesReadOnlyAndDestructiveTools() {
        Map<String, McpSchema.ToolAnnotations> hints = new HashMap<>();
        client.listTools().tools().forEach(t -> hints.put(t.name(), t.annotations()));

        assertThat(hints.get("list_departments").readOnlyHint()).isTrue();
        assertThat(hints.get("search_employees").readOnlyHint()).isTrue();
        assertThat(hints.get("delete_employee").destructiveHint()).isTrue();
        assertThat(hints.get("delete_department").destructiveHint()).isTrue();
        assertThat(hints.get("transfer_employee").readOnlyHint()).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    void createEmployeeSchemaMarksRequiredFields() {
        var tool = client.listTools().tools().stream().filter(t -> t.name().equals("create_employee"))
                .findFirst().orElseThrow();

        assertThat((List<String>) tool.inputSchema().get("required"))
                .contains("firstName", "lastName", "email", "jobTitle", "salary", "hireDate")
                .doesNotContain("departmentId");
    }

    /**
     * Guards the public tool contract. Regenerate after an intentional change with
     * {@code ./mvnw test -Dtest=McpServerContractTest -DupdateSnapshot=true} and review the diff.
     */
    @Test
    void toolContractsMatchSnapshot() throws Exception {
        var mapper = JsonMapper.builder()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                .build();
        var contracts = client.listTools().tools().stream()
                .sorted(Comparator.comparing(McpSchema.Tool::name))
                .map(t -> Map.of("name", t.name(), "description", t.description(),
                        "annotations", t.annotations(), "inputSchema", t.inputSchema()))
                .toList();
        String actual = mapper.writeValueAsString(contracts);
        Path snapshot = Path.of("src/test/resources/mcp/tools-snapshot.json");

        if (Boolean.getBoolean("updateSnapshot") || !Files.exists(snapshot)) {
            Files.createDirectories(snapshot.getParent());
            Files.writeString(snapshot, actual);
        }
        assertThat(actual).isEqualToIgnoringNewLines(Files.readString(snapshot));
    }

    // --- department tools ----------------------------------------------------------------------------------

    @Test
    void listDepartmentsReturnsSeededDepartments() {
        assertThat(text(ok("list_departments", Map.of()))).contains("ENG", "HR", "FIN", "SALES");
    }

    @Test
    void readDepartmentTools() {
        assertThat(text(ok("get_department", Map.of("departmentId", 1)))).contains("Engineering");
        assertThat(text(ok("get_department_employees", Map.of("departmentId", 2)))).contains("Priya");
        assertThat(text(ok("get_department_summary", Map.of("departmentId", 4))))
                .contains("\"headcount\":3", "Rohan Gupta");
    }

    @Test
    void departmentLifecycle() {
        var created = text(ok("create_department",
                Map.of("code", "LEGAL", "name", "Legal", "location", "Delhi")));
        assertThat(created).contains("\"code\":\"LEGAL\"");
        long id = Long.parseLong(created.replaceAll(".*\"id\":(\\d+).*", "$1"));

        assertThat(text(ok("update_department", Map.of("departmentId", id, "location", "Gurugram"))))
                .contains("Gurugram", "\"name\":\"Legal\"");
        assertThat(text(ok("delete_department", Map.of("departmentId", id)))).contains("deleted");
    }

    @Test
    void assignDepartmentManager() {
        assertThat(text(ok("assign_department_manager", Map.of("departmentId", 2, "employeeId", 6))))
                .contains("Karan Mehta");
    }

    // --- employee tools ------------------------------------------------------------------------------------

    @Test
    void searchEmployees() {
        assertThat(text(ok("search_employees", Map.of("name", "rao")))).contains("asha.rao@example.com");
        assertThat(text(ok("search_employees", Map.of()))).contains("\"totalElements\"");
        assertThat(text(ok("search_employees", Map.of("status", "ON_LEAVE")))).contains("Rahul");
    }

    @Test
    void getEmployee() {
        assertThat(text(ok("get_employee", Map.of("employeeId", 1)))).contains("Asha Rao", "\"code\":\"ENG\"");
    }

    @Test
    void employeeLifecycle() {
        var created = text(ok("create_employee", Map.of(
                "firstName", "Tara", "lastName", "Bose", "email", "tara.bose@example.com",
                "jobTitle", "QA Engineer", "salary", 88000, "hireDate", "2026-02-01", "departmentId", 1)));
        assertThat(created).contains("\"status\":\"ACTIVE\"", "\"code\":\"ENG\"");
        long id = Long.parseLong(created.replaceAll("^\\{\"id\":(\\d+).*", "$1"));

        assertThat(text(ok("update_employee", Map.of("employeeId", id, "jobTitle", "Senior QA Engineer"))))
                .contains("Senior QA Engineer", "tara.bose@example.com");
        assertThat(text(ok("transfer_employee", Map.of("employeeId", id, "targetDepartmentId", 3))))
                .contains("\"code\":\"FIN\"");
        assertThat(text(ok("delete_employee", Map.of("employeeId", id)))).contains("deleted");
    }

    // --- error behaviour -----------------------------------------------------------------------------------

    @Test
    void unknownEmployeeIsToolErrorWithGuidance() {
        assertThat(text(error("get_employee", Map.of("employeeId", 999999))))
                .contains("Employee 999999 not found", "search_employees");
    }

    @Test
    void deletingNonEmptyDepartmentIsToolError() {
        assertThat(text(error("delete_department", Map.of("departmentId", 1))))
                .contains("Transfer them before deleting");
    }

    @Test
    void duplicateEmailIsToolError() {
        assertThat(text(error("create_employee", Map.of(
                "firstName", "Asha", "lastName", "Rao", "email", "asha.rao@example.com",
                "jobTitle", "Engineer", "salary", 1000, "hireDate", "2026-01-01"))))
                .contains("already exists");
    }

    @Test
    void invalidInputIsToolErrorNamingTheField() {
        assertThat(text(error("create_employee", Map.of(
                "firstName", "X", "lastName", "Y", "email", "not-an-email",
                "jobTitle", "Engineer", "salary", 1000, "hireDate", "2026-01-01"))))
                .contains("email");
    }

    @Test
    void transferringTerminatedEmployeeIsToolError() {
        assertThat(text(error("transfer_employee", Map.of("employeeId", 9, "targetDepartmentId", 1))))
                .contains("TERMINATED");
    }

    @Test
    void errorsDoNotLeakInternals() {
        var message = text(error("get_employee", Map.of("employeeId", 999999)));

        assertThat(message).doesNotContain("Exception", "com.restmcp", "SQL");
    }

    // --- resources and prompts -----------------------------------------------------------------------------

    @Test
    void departmentDirectoryResource() {
        assertThat(client.listResources().resources()).extracting(McpSchema.Resource::uri)
                .contains("org://departments");

        var contents = client.readResource(new McpSchema.ReadResourceRequest("org://departments")).contents();

        assertThat(((McpSchema.TextResourceContents) contents.getFirst()).text()).contains("Engineering");
    }

    @Test
    void departmentRosterResourceTemplate() {
        assertThat(client.listResourceTemplates().resourceTemplates())
                .extracting(McpSchema.ResourceTemplate::uriTemplate)
                .contains("org://departments/{code}/roster");

        var contents = client.readResource(new McpSchema.ReadResourceRequest("org://departments/SALES/roster"))
                .contents();

        assertThat(((McpSchema.TextResourceContents) contents.getFirst()).text()).contains("Rohan");
    }

    @Test
    void prompts() {
        assertThat(client.listPrompts().prompts()).extracting(McpSchema.Prompt::name)
                .contains("department_report", "onboard_employee");

        var prompt = client.getPrompt(new McpSchema.GetPromptRequest("department_report",
                Map.of("departmentCode", "ENG")));

        assertThat(((McpSchema.TextContent) prompt.messages().getFirst().content()).text())
                .contains("ENG", "get_department_summary");
    }
}
