package com.taskmgmt.service;

import com.taskmgmt.dto.AssigneeDto;
import com.taskmgmt.dto.TaskRequestDto;
import com.taskmgmt.dto.TaskResponseDto;
import com.taskmgmt.entity.*;
import com.taskmgmt.repository.TaskAssigneeRepository;
import com.taskmgmt.repository.TaskRepository;
import com.taskmgmt.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


public class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TaskAssigneeRepository taskAssigneeRepository;

    @InjectMocks
    private TaskService taskService;

    private AutoCloseable mocks;

    @BeforeEach
    void setup() {
        mocks = MockitoAnnotations.openMocks(this);
        // instantiate service explicitly in case @InjectMocks isn't used as desired:
        taskService = new TaskService(taskRepository, userRepository, taskAssigneeRepository);
    }

    // helper builders
    private User makeUser(Long id, String name, Role role, String email) {
        return User.builder()
                .id(id)
                .name(name)
                .email(email)
                .role(role)
                .build();
    }

    private Task makeTask(Long id, String title, User createdBy, TaskStatus status, LocalDate dueDate) {
        return Task.builder()
                .id(id)
                .title(title)
                .description(title + " desc")
                .status(status)
                .dueDate(dueDate)
                .createdAt(Instant.now())
                .createdBy(createdBy)
                .build();
    }

    /*
     * createTaskByAdmin
     */

    @Test
    void createTaskByAdmin_success() {
        User assignee = makeUser(2L, "Assignee", Role.USER, "assignee@test.com");
        User admin = makeUser(1L, "Admin", Role.ADMIN, "admin@test.com");

        TaskRequestDto dto = new TaskRequestDto();
        dto.setTitle("New Task");
        dto.setDescription("desc");
        dto.setDueDate(LocalDate.now().plusDays(3));
        dto.setUserId(assignee.getId());
        dto.setStatus(TaskStatus.TODO);
        dto.setCreatedById(admin.getId());

        when(userRepository.findById(assignee.getId())).thenReturn(Optional.of(assignee));
        when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));

        // saved task returned by repository should contain id
        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        Task saved = makeTask(10L, dto.getTitle(), admin, TaskStatus.TODO, dto.getDueDate());
        when(taskRepository.save(any(Task.class))).thenReturn(saved);

        TaskResponseDto response = taskService.createTaskByAdmin(dto, admin.getEmail());

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals(dto.getTitle(), response.getTitle());
        assertEquals(dto.getDueDate(), response.getDueDate());
        assertEquals(admin.getId(), response.getCreatedById());
        verify(userRepository, times(1)).findById(assignee.getId());
        verify(userRepository, times(1)).findByEmail(admin.getEmail());
        verify(taskRepository, times(1)).save(taskCaptor.capture());
        verify(taskAssigneeRepository, times(1)).save(any(TaskAssignee.class));

        Task capturedTask = taskCaptor.getValue();
        assertEquals(dto.getTitle(), capturedTask.getTitle());
        assertEquals(dto.getDescription(), capturedTask.getDescription());
        assertEquals(TaskStatus.TODO, capturedTask.getStatus());
    }

    @Test
    void createTaskByAdmin_userNotFound_throws() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        TaskRequestDto dto = new TaskRequestDto();
        dto.setUserId(999L);

        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(makeUser(1L, "Admin", Role.ADMIN, "admin@test.com")));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> taskService.createTaskByAdmin(dto, "admin@test.com"));
        assertTrue(ex.getMessage().contains("User not found"));
        verify(taskRepository, never()).save(any());
    }

    @Test
    void createTaskByAdmin_adminNotFound_throws() {
        User assignee = makeUser(2L, "Assignee", Role.USER, "assignee@test.com");
        when(userRepository.findById(assignee.getId())).thenReturn(Optional.of(assignee));
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.empty());

        TaskRequestDto dto = new TaskRequestDto();
        dto.setUserId(assignee.getId());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> taskService.createTaskByAdmin(dto, "admin@test.com"));
        assertTrue(ex.getMessage().contains("Admin not found"));
        verify(taskRepository, never()).save(any());
    }

    /*
     * getTasksForUser
     */

    @Test
    void getTasksForUser_success() {
        User user = makeUser(2L, "Some User", Role.USER, "a@b.test");
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        Task t1 = makeTask(1L, "T1", makeUser(10L, "Creator", Role.USER, "c@test"), TaskStatus.TODO, LocalDate.now());
        Task t2 = makeTask(2L, "T2", makeUser(11L, "Creator2", Role.USER, "c2@test"), TaskStatus.IN_PROGRESS, LocalDate.now());

        TaskAssignee a1 = TaskAssignee.builder().id(101L).task(t1).user(user).build();
        TaskAssignee a2 = TaskAssignee.builder().id(102L).task(t2).user(user).build();

        when(taskAssigneeRepository.findByUser(user)).thenReturn(Arrays.asList(a1, a2));

        List<TaskResponseDto> dtos = taskService.getTasksForUser(user.getEmail());
        assertEquals(2, dtos.size());
        assertEquals("T1", dtos.get(0).getTitle());
        assertEquals("T2", dtos.get(1).getTitle());
        verify(taskAssigneeRepository, times(1)).findByUser(user);
    }

    @Test
    void getTasksForUser_userNotFound_throws() {
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> taskService.getTasksForUser("missing@test.com"));
        assertTrue(ex.getMessage().contains("User not found"));
    }

    /*
     * getAllTasks
     */

    @Test
    void getAllTasks_success() {
        User creator = makeUser(1L, "Creator", Role.USER, "c@test");
        Task t1 = makeTask(1L, "A", creator, TaskStatus.TODO, LocalDate.now());
        Task t2 = makeTask(2L, "B", creator, TaskStatus.IN_PROGRESS, LocalDate.now());
        when(taskRepository.findAll()).thenReturn(Arrays.asList(t1, t2));

        List<TaskResponseDto> all = taskService.getAllTasks();
        assertEquals(2, all.size());
        assertEquals("A", all.get(0).getTitle());
        assertEquals("B", all.get(1).getTitle());
        verify(taskRepository, times(1)).findAll();
    }

    /*
     * getFilteredTasks
     */

    @Test
    void getFilteredTasks_success() {
        Task t = makeTask(1L, "Filtered", makeUser(1L, "Creator", Role.USER, "c@test"), TaskStatus.TODO, LocalDate.now());
        when(taskRepository.findFilteredTasks(TaskStatus.TODO, LocalDate.now(), "assignee@test.com"))
                .thenReturn(Collections.singletonList(t));

        List<TaskResponseDto> res = taskService.getFilteredTasks(TaskStatus.TODO, LocalDate.now(), "assignee@test.com");
        assertEquals(1, res.size());
        assertEquals("Filtered", res.get(0).getTitle());
    }

    /*
     * updateTaskStatus
     */

    @Test
    void updateTaskStatus_success_byAdmin() {
        User admin = makeUser(1L, "Admin", Role.ADMIN, "admin@test.com");
        Task task = makeTask(10L, "Task", makeUser(2L, "Owner", Role.USER, "owner@test.com"), TaskStatus.TODO, LocalDate.now());
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        when(taskRepository.save(any(Task.class))).thenAnswer(i -> i.getArgument(0));

        TaskResponseDto dto = taskService.updateTaskStatus(10L, TaskStatus.DONE, admin.getEmail());
        assertEquals(TaskStatus.DONE.name(), dto.getStatus());
        verify(taskRepository, times(1)).save(task);
    }

    @Test
    void updateTaskStatus_success_byOwner() {
        User owner = makeUser(2L, "Owner", Role.USER, "owner@test.com");
        Task task = makeTask(10L, "Task", owner, TaskStatus.TODO, LocalDate.now());
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(userRepository.findByEmail(owner.getEmail())).thenReturn(Optional.of(owner));
        when(taskRepository.save(any(Task.class))).thenAnswer(i -> i.getArgument(0));

        TaskResponseDto dto = taskService.updateTaskStatus(10L, TaskStatus.IN_PROGRESS, owner.getEmail());
        assertEquals(TaskStatus.IN_PROGRESS.name(), dto.getStatus());
        verify(taskRepository, times(1)).save(task);
    }

    @Test
    void updateTaskStatus_success_byAssignee() {
        User assignee = makeUser(3L, "Assignee", Role.USER, "assignee@test.com");
        Task task = makeTask(10L, "Task", makeUser(2L, "Owner", Role.USER, "owner@test.com"), TaskStatus.TODO, LocalDate.now());
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(userRepository.findByEmail(assignee.getEmail())).thenReturn(Optional.of(assignee));
        when(taskAssigneeRepository.findByTaskAndUser(task, assignee)).thenReturn(Optional.of(TaskAssignee.builder().id(201L).task(task).user(assignee).build()));
        when(taskRepository.save(any(Task.class))).thenAnswer(i -> i.getArgument(0));

        TaskResponseDto dto = taskService.updateTaskStatus(10L, TaskStatus.IN_PROGRESS, assignee.getEmail());
        assertEquals(TaskStatus.IN_PROGRESS.name(), dto.getStatus());
    }

    @Test
    void updateTaskStatus_unauthorized_throws() {
        User stranger = makeUser(9L, "Stranger", Role.USER, "s@test.com");
        Task task = makeTask(10L, "Task", makeUser(2L, "Owner", Role.USER, "owner@test.com"), TaskStatus.TODO, LocalDate.now());

        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(userRepository.findByEmail(stranger.getEmail())).thenReturn(Optional.of(stranger));
        when(taskAssigneeRepository.findByTaskAndUser(task, stranger)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> taskService.updateTaskStatus(10L, TaskStatus.IN_PROGRESS, stranger.getEmail()));
        assertTrue(ex.getMessage().contains("Only task owner, assignee, or admin can update status"));
    }

    @Test
    void updateTaskStatus_taskNotFound_throws() {
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());
        when(userRepository.findByEmail("u@test.com")).thenReturn(Optional.of(makeUser(1L, "U", Role.USER, "u@test.com")));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> taskService.updateTaskStatus(999L, TaskStatus.IN_PROGRESS, "u@test.com"));
        assertTrue(ex.getMessage().contains("Task not found"));
    }

    @Test
    void updateTaskStatus_userNotFound_throws() {
        Task task = makeTask(10L, "Task", makeUser(2L, "Owner", Role.USER, "owner@test.com"), TaskStatus.TODO, LocalDate.now());
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> taskService.updateTaskStatus(10L, TaskStatus.IN_PROGRESS, "missing@test.com"));
        assertTrue(ex.getMessage().contains("User not found"));
    }

    @Test
    void updateTaskStatus_doneChangeNotAllowed_nonAdmin_throws() {
        User owner = makeUser(2L, "Owner", Role.USER, "owner@test.com");
        Task task = makeTask(10L, "Task", owner, TaskStatus.DONE, LocalDate.now());
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(userRepository.findByEmail(owner.getEmail())).thenReturn(Optional.of(owner));
        // owner is not admin -> cannot change status once DONE
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> taskService.updateTaskStatus(10L, TaskStatus.IN_PROGRESS, owner.getEmail()));
        assertTrue(ex.getMessage().contains("Only admin can change the status once the task is DONE"));
    }

    @Test
    void updateTaskStatus_doneToInProgress_nonAdmin_throws() {
        User assignee = makeUser(3L, "Assignee", Role.USER, "a@test.com");
        Task task = makeTask(10L, "Task", makeUser(2L, "Owner", Role.USER, "owner@test.com"), TaskStatus.DONE, LocalDate.now());
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(userRepository.findByEmail(assignee.getEmail())).thenReturn(Optional.of(assignee));
        when(taskAssigneeRepository.findByTaskAndUser(task, assignee)).thenReturn(Optional.of(TaskAssignee.builder().id(201L).task(task).user(assignee).build()));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> taskService.updateTaskStatus(10L, TaskStatus.IN_PROGRESS, assignee.getEmail()));
        assertTrue(ex.getMessage().contains("Only admin can move DONE task back to IN_PROGRESS"));
    }

    /*
     * assignUsersToTask
     *
     * NOTE: Service contains a potential bug:
     *   if (!task.getCreatedBy().equals(loggedInUser.getId()) ...
     * This compares a User to a Long and will be false; therefore an owner attempt will
     * be treated as unauthorized. Tests below reflect current behavior.
     */

    @Test
    void assignUsersToTask_success_byAdmin() {
        User owner = makeUser(2L, "Owner", Role.USER, "owner@test.com");
        User admin = makeUser(1L, "Admin", Role.ADMIN, "admin@test.com");
        Task task = makeTask(100L, "Task100", owner, TaskStatus.TODO, LocalDate.now());

        when(taskRepository.findById(100L)).thenReturn(Optional.of(task));
        when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        // assign user id 5
        User userToAssign = makeUser(5L, "New", Role.USER, "new@test.com");
        when(userRepository.findById(5L)).thenReturn(Optional.of(userToAssign));
        when(taskAssigneeRepository.existsByTaskAndUser(task, userToAssign)).thenReturn(false);

        List<AssigneeDto> assigned = taskService.assignUsersToTask(100L, Collections.singletonList(5L), admin.getEmail());
        assertEquals(1, assigned.size());
        assertEquals(userToAssign.getId(), assigned.get(0).getId());
        verify(taskAssigneeRepository, times(1)).save(any(TaskAssignee.class));
    }

    @Test
    void assignUsersToTask_ownerAttempt_fails_dueToImplementation_bug() {
        // Because service compares User to Long (bug), owner is not recognized and should get RuntimeException
        User owner = makeUser(2L, "Owner", Role.USER, "owner@test.com");
        Task task = makeTask(100L, "Task100", owner, TaskStatus.TODO, LocalDate.now());

        when(taskRepository.findById(100L)).thenReturn(Optional.of(task));
        when(userRepository.findByEmail(owner.getEmail())).thenReturn(Optional.of(owner));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> taskService.assignUsersToTask(100L, Collections.singletonList(5L), owner.getEmail()));
        assertTrue(ex.getMessage().contains("Only task owner or admin can assign users"));
    }

    @Test
    void assignUsersToTask_taskNotFound_throws() {
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(makeUser(1L, "Admin", Role.ADMIN, "admin@test.com")));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> taskService.assignUsersToTask(999L, Collections.singletonList(5L), "admin@test.com"));
        assertTrue(ex.getMessage().contains("Task not found"));
    }

    @Test
    void assignUsersToTask_loggedInUserNotFound_throws() {
        User owner = makeUser(2L, "Owner", Role.USER, "owner@test.com");
        Task task = makeTask(100L, "Task100", owner, TaskStatus.TODO, LocalDate.now());
        when(taskRepository.findById(100L)).thenReturn(Optional.of(task));
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> taskService.assignUsersToTask(100L, Collections.singletonList(5L), "missing@test.com"));
        assertTrue(ex.getMessage().contains("User not found"));
    }

    @Test
    void assignUsersToTask_userIdNotFound_throws() {
        User admin = makeUser(1L, "Admin", Role.ADMIN, "admin@test.com");
        User owner = makeUser(2L, "Owner", Role.USER, "owner@test.com");
        Task task = makeTask(100L, "Task100", owner, TaskStatus.TODO, LocalDate.now());

        when(taskRepository.findById(100L)).thenReturn(Optional.of(task));
        when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> taskService.assignUsersToTask(100L, Collections.singletonList(99L), admin.getEmail()));
        assertTrue(ex.getMessage().contains("User not found with id: 99"));
    }

    @Test
    void assignUsersToTask_alreadyAssigned_skipsSavingButReturnsDto() {
        User admin = makeUser(1L, "Admin", Role.ADMIN, "admin@test.com");
        User owner = makeUser(2L, "Owner", Role.USER, "owner@test.com");
        Task task = makeTask(100L, "Task100", owner, TaskStatus.TODO, LocalDate.now());
        User existing = makeUser(5L, "Existing", Role.USER, "e@test.com");

        when(taskRepository.findById(100L)).thenReturn(Optional.of(task));
        when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        when(userRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(taskAssigneeRepository.existsByTaskAndUser(task, existing)).thenReturn(true);

        List<AssigneeDto> assigned = taskService.assignUsersToTask(100L, Collections.singletonList(existing.getId()), admin.getEmail());
        assertEquals(1, assigned.size());
        assertEquals(existing.getId(), assigned.get(0).getId());
        // save should not be invoked since already assigned
        verify(taskAssigneeRepository, never()).save(argThat(arg -> arg.getUser().equals(existing)));
    }

    /*
     * getOverdueTasks
     */

    @Test
    void getOverdueTasks_adminFlow() {
        User admin = makeUser(1L, "Admin", Role.ADMIN, "admin@test.com");
        when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));

        Task overdue = makeTask(1L, "Overdue", makeUser(2L, "Owner", Role.USER, "o@test.com"), TaskStatus.TODO, LocalDate.now().minusDays(5));
        when(taskRepository.findOverdueTasksForAdmin(LocalDate.now())).thenReturn(Collections.singletonList(overdue));

        List<TaskResponseDto> result = taskService.getOverdueTasks(admin.getEmail());
        assertEquals(1, result.size());
        assertEquals("Overdue", result.get(0).getTitle());
    }

    @Test
    void getOverdueTasks_userFlow() {
        User user = makeUser(3L, "User", Role.USER, "user@test.com");
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        Task overdue = makeTask(2L, "UserOverdue", makeUser(2L, "Owner", Role.USER, "o@test.com"), TaskStatus.TODO, LocalDate.now().minusDays(2));
        when(taskRepository.findOverdueTasksForUser(user.getEmail(), LocalDate.now())).thenReturn(Collections.singletonList(overdue));

        List<TaskResponseDto> result = taskService.getOverdueTasks(user.getEmail());
        assertEquals(1, result.size());
        assertEquals("UserOverdue", result.get(0).getTitle());
    }

    @Test
    void getOverdueTasks_userNotFound_throws() {
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> taskService.getOverdueTasks("missing@test.com"));
        assertTrue(ex.getMessage().contains("User not found"));
    }
}