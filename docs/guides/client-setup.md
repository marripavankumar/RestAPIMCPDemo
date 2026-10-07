# Connecting MCP clients

The server exposes the same 14 tools, 2 resources and 2 prompts over three transports:

| Transport | Start command | Endpoint | Use for |
|---|---|---|---|
| Streamable HTTP (default) | `java -jar target/rest-api-mcp-demo-0.0.1-SNAPSHOT.jar` | `http://localhost:8080/mcp` | Claude Code, VS Code, Cursor, Inspector, remote agents |
| STDIO | the host launches `java -jar <jar> --spring.profiles.active=stdio` | stdin/stdout | Claude Desktop and other hosts that run local processes |
| Legacy HTTP+SSE | `java -jar <jar> --spring.profiles.active=legacy-sse` | `http://localhost:8080/sse` | Older clients that predate Streamable HTTP |

Build the JAR once with `./mvnw package` (`.\mvnw.cmd package` in PowerShell).

> **STDIO and data:** the default database is in-memory H2, so each STDIO launch starts from the seed data and
> doesn't share state with a separately running HTTP server. Use the `postgres` profile (Phase 7) for shared, persistent data.

---

## MCP Inspector (testing)

```bash
npx @modelcontextprotocol/inspector
```

In the UI, choose **Streamable HTTP**, enter `http://localhost:8080/mcp` and click **Connect**. The Tools, Resources and Prompts
tabs show everything the server offers.

Headless (CLI mode), useful in scripts:

```bash
npx @modelcontextprotocol/inspector --cli http://localhost:8080/mcp --transport http --method tools/list
npx @modelcontextprotocol/inspector --cli http://localhost:8080/mcp --transport http \
    --method tools/call --tool-name get_department_summary --tool-arg departmentId=1
npx @modelcontextprotocol/inspector --cli http://localhost:8080/mcp --transport http \
    --method resources/read --uri org://departments/HR/roster
```

## Claude Code

This repo has a project-scoped [`.mcp.json`](../../.mcp.json). Start the server, open Claude Code in the repo folder and
approve the `emp-dept` server when asked. To add it to your own user scope instead:

```bash
claude mcp add --transport http emp-dept http://localhost:8080/mcp
claude mcp list
```

Try: *"Which department has the highest average salary?"* or `/mcp` to see the server's status.

With an API key, add `"headers": {"X-API-KEY": "<key>"}` to the server entry. Verified on 2026-10-07 with Claude Code 2.1.292
in headless mode (`claude -p ... --mcp-config <file>`) against the containerized server on PostgreSQL:

| Prompt | Tools called | Result |
|---|---|---|
| Which department has the highest average salary, and who manages it? | `list_departments`, `get_department_summary` ×4 | "Engineering, 137,500, managed by Asha Rao" (correct) |
| Move Sneha Kulkarni to Engineering | `search_employees`, `list_departments`, `transfer_employee` | Tool error relayed: TERMINATED employees can't be transferred |
| Move Meera Iyer to Sales | `search_employees`, `list_departments`, `transfer_employee` | Transferred; confirmed via REST |

## Claude Desktop (STDIO)

Edit `%APPDATA%\Claude\claude_desktop_config.json` (Windows) or
`~/Library/Application Support/Claude/claude_desktop_config.json` (macOS). If Claude Desktop came from the Microsoft
Store, the Windows file is `%LOCALAPPDATA%PackagesClaude_pzs8sxrjxfjjcLocalCacheRoamingClaudeclaude_desktop_config.json`.
Add the `mcpServers` key next to the existing settings. Forward slashes work in Windows paths and avoid JSON escaping:

```json
{
  "mcpServers": {
    "emp-dept": {
      "command": "java",
      "args": [
        "-jar",
        "E:\\Pavan\\workspace\\RestAPIMCPDemo\\target\\rest-api-mcp-demo-0.0.1-SNAPSHOT.jar",
        "--spring.profiles.active=stdio"
      ]
    }
  }
}
```

If `java` isn't on the PATH, use the full path to a JDK 21 `java.exe`, e.g. `"command": "C:/Users/<you>/.jdks/ms-21.0.12.1/bin/java.exe"`.
Don't run `mvnw clean` while Claude Desktop is open, because it deletes the JAR the app launches. Restart Claude Desktop. The tools show up in
the tools menu. Logs go to `%TEMP%\rest-api-mcp-demo-stdio.log`.

Or connect Claude Desktop to the running HTTP server through the `mcp-remote` bridge:

```json
{
  "mcpServers": {
    "emp-dept": { "command": "npx", "args": ["-y", "mcp-remote", "http://localhost:8080/mcp"] }
  }
}
```

## VS Code (GitHub Copilot agent mode)

This repo has [`.vscode/mcp.json`](../../.vscode/mcp.json). Open the folder, start the server, then in Copilot Chat
switch to **Agent** mode. The `emp-dept` tools appear in the tools picker.

## Cursor

`~/.cursor/mcp.json` or `.cursor/mcp.json` in the project:

```json
{ "mcpServers": { "emp-dept": { "url": "http://localhost:8080/mcp" } } }
```

## Spring AI MCP client (Java agents)

```yaml
spring:
  ai:
    mcp:
      client:
        streamable-http:
          connections:
            emp-dept:
              url: http://localhost:8080
              endpoint: /mcp
```

## Prompts to try

| Prompt | Tools the model should use |
|---|---|
| "List the departments and their managers" | `list_departments` |
| "Who in Engineering is on leave?" | `search_employees` (departmentId + status) |
| "Move Meera Iyer to Finance" | `search_employees`, `list_departments`, `transfer_employee` |
| "Hire Tara Bose as a QA engineer in ENG at 88,000 starting today" | `list_departments`, `create_employee` |
| "Can we delete the Sales department?" | `delete_department`, which returns an error explaining it must be emptied first |
| `/department_report ENG` (prompt) | `get_department_summary`, `get_department_employees` |

## Troubleshooting

| Symptom | Fix |
|---|---|
| `404` on `/mcp` | `spring.ai.mcp.server.protocol: STREAMABLE` is missing. Spring AI 2.0 falls back to SSE without it. |
| STDIO client says "invalid JSON" | Something is writing to stdout. Make sure the `stdio` profile is active so the banner and console logging are off. |
| Claude Desktop shows no tools and `mcpServers` has vanished from the config | Desktop rewrites `claude_desktop_config.json` when it quits, dropping edits made while it was running. Quit it fully (tray → Quit, or end every `Claude.exe` under `WindowsApps\Claude_*`), edit the file, then start it again. Or edit through **Settings → Developer → Edit Config**. |
| Claude Desktop shows no tools | Check that the JAR path is absolute and `java` is JDK 21+, then read `%TEMP%\rest-api-mcp-demo-stdio.log` |
| `401` (after Phase 7) | Send the `X-API-KEY` header. See the security section of the README. |
