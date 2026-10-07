package com.restmcp.demo.employee;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.restmcp.demo.common.api.PageResponse;
import com.restmcp.demo.common.exception.BusinessRuleException;
import com.restmcp.demo.common.exception.ConflictException;
import com.restmcp.demo.common.exception.NotFoundException;
import com.restmcp.demo.employee.dto.EmployeeResponse;
import com.restmcp.demo.employee.dto.EmployeeSearchCriteria;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmployeeController.class)
class EmployeeControllerTest {

    static final String VALID_BODY = """
            {"firstName":"Asha","lastName":"Rao","email":"asha.rao@example.com","jobTitle":"Engineer",
             "salary":95000.00,"hireDate":"2025-09-01","departmentId":1}
            """;

    static final EmployeeResponse ASHA = new EmployeeResponse(42L, "Asha", "Rao", "Asha Rao",
            "asha.rao@example.com", "Engineer", new BigDecimal("95000.00"), LocalDate.of(2025, 9, 1),
            EmployeeStatus.ACTIVE, new EmployeeResponse.DepartmentRef(1L, "ENG", "Engineering"));

    @Autowired
    MockMvc mvc;

    @MockitoBean
    EmployeeService service;

    @Test
    void getById_returnsEmployee() throws Exception {
        when(service.findById(42L)).thenReturn(ASHA);

        mvc.perform(get("/api/v1/employees/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Asha Rao"))
                .andExpect(jsonPath("$.department.code").value("ENG"));
    }

    @Test
    void getById_unknown_returns404ProblemDetail() throws Exception {
        when(service.findById(99L)).thenThrow(new NotFoundException("Employee 99 not found."));

        mvc.perform(get("/api/v1/employees/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.detail").value("Employee 99 not found."));
    }

    @Test
    void search_bindsCriteriaAndPaging() throws Exception {
        when(service.search(any(), any())).thenReturn(new PageResponse<>(List.of(ASHA), 0, 5, 1, 1));

        mvc.perform(get("/api/v1/employees").param("name", "rao").param("status", "ACTIVE").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("asha.rao@example.com"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(service).search(eq(new EmployeeSearchCriteria("rao", null, EmployeeStatus.ACTIVE)),
                any(Pageable.class));
    }

    @Test
    void search_invalidStatus_returns400() throws Exception {
        mvc.perform(get("/api/v1/employees").param("status", "RETIRED"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_valid_returns201WithLocation() throws Exception {
        when(service.create(any())).thenReturn(ASHA);

        mvc.perform(post("/api/v1/employees").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/employees/42"))
                .andExpect(jsonPath("$.id").value(42));
    }

    @Test
    void create_invalid_returns400WithFieldErrors() throws Exception {
        mvc.perform(post("/api/v1/employees").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"","lastName":"Rao","email":"not-an-email","jobTitle":"Engineer",
                                 "salary":-5,"hireDate":"2025-09-01"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[?(@.field == 'email')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'firstName')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'salary')]").exists());
    }

    @Test
    void create_duplicateEmail_returns409() throws Exception {
        when(service.create(any())).thenThrow(new ConflictException("An employee with email 'x' already exists."));

        mvc.perform(post("/api/v1/employees").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("An employee with email 'x' already exists."));
    }

    @Test
    void update_returns200() throws Exception {
        when(service.update(eq(42L), any())).thenReturn(ASHA);

        mvc.perform(put("/api/v1/employees/42").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk());
    }

    @Test
    void delete_returns204() throws Exception {
        mvc.perform(delete("/api/v1/employees/42")).andExpect(status().isNoContent());

        verify(service).delete(42L);
    }

    @Test
    void transfer_businessRuleViolation_returns422() throws Exception {
        when(service.transfer(42L, 1L)).thenThrow(new BusinessRuleException("already in department ENG"));

        mvc.perform(put("/api/v1/employees/42/department/1"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("already in department ENG"));
    }

    @Test
    void unexpectedError_returns500WithoutInternals() throws Exception {
        when(service.findById(1L)).thenThrow(new IllegalStateException("SQL boom at com.example.Internal"));

        mvc.perform(get("/api/v1/employees/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred."));
    }
}
