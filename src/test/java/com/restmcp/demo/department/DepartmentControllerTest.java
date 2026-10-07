package com.restmcp.demo.department;

import java.math.BigDecimal;
import java.util.List;

import com.restmcp.demo.common.exception.ConflictException;
import com.restmcp.demo.common.exception.NotFoundException;
import com.restmcp.demo.department.dto.DepartmentResponse;
import com.restmcp.demo.department.dto.DepartmentSummary;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DepartmentController.class)
class DepartmentControllerTest {

    static final DepartmentResponse ENG = new DepartmentResponse(1L, "ENG", "Engineering", "Bengaluru",
            10L, "Asha Rao", 4);

    @Autowired
    MockMvc mvc;

    @MockitoBean
    DepartmentService service;

    @Test
    void list_returnsDepartments() throws Exception {
        when(service.findAll()).thenReturn(List.of(ENG));

        mvc.perform(get("/api/v1/departments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("ENG"))
                .andExpect(jsonPath("$[0].headcount").value(4));
    }

    @Test
    void get_unknown_returns404() throws Exception {
        when(service.findById(9L)).thenThrow(new NotFoundException("Department 9 not found."));

        mvc.perform(get("/api/v1/departments/9")).andExpect(status().isNotFound());
    }

    @Test
    void create_returns201() throws Exception {
        when(service.create(any())).thenReturn(ENG);

        mvc.perform(post("/api/v1/departments").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"ENG\",\"name\":\"Engineering\",\"location\":\"Bengaluru\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/departments/1"));
    }

    @Test
    void create_invalidCode_returns400() throws Exception {
        mvc.perform(post("/api/v1/departments").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"E N G!\",\"name\":\"Engineering\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("code"));
    }

    @Test
    void update_returns200() throws Exception {
        when(service.update(eq(1L), any())).thenReturn(ENG);

        mvc.perform(put("/api/v1/departments/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"ENG\",\"name\":\"Engineering\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void delete_withEmployees_returns409() throws Exception {
        org.mockito.Mockito.doThrow(new ConflictException("Department ENG still has 4 employees."))
                .when(service).delete(1L);

        mvc.perform(delete("/api/v1/departments/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Department ENG still has 4 employees."));
    }

    @Test
    void delete_empty_returns204() throws Exception {
        mvc.perform(delete("/api/v1/departments/5")).andExpect(status().isNoContent());

        verify(service).delete(5L);
    }

    @Test
    void employees_returnsMembers() throws Exception {
        when(service.employeesOf(1L)).thenReturn(List.of());

        mvc.perform(get("/api/v1/departments/1/employees")).andExpect(status().isOk());
    }

    @Test
    void summary_returnsStats() throws Exception {
        when(service.summary(1L)).thenReturn(new DepartmentSummary(1L, "ENG", "Engineering", 4,
                new BigDecimal("550000.00"), new BigDecimal("137500.00"), "Asha Rao"));

        mvc.perform(get("/api/v1/departments/1/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageSalary").value(137500.00));
    }

    @Test
    void assignManager_returns200() throws Exception {
        when(service.assignManager(1L, 2L)).thenReturn(ENG);

        mvc.perform(put("/api/v1/departments/1/manager/2")).andExpect(status().isOk());
    }
}
