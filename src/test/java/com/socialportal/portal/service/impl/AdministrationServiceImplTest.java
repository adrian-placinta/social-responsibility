package com.socialportal.portal.service.impl;

import com.socialportal.portal.exception.issue.NoIssueFoundException;
import com.socialportal.portal.exception.user.NoRolesDataBase;
import com.socialportal.portal.exception.user.NoUserFoundException;
import com.socialportal.portal.model.issues.Issue;
import com.socialportal.portal.model.user.Roles;
import com.socialportal.portal.model.user.UserEntity;
import com.socialportal.portal.repository.IssueRepository;
import com.socialportal.portal.repository.RoleRepository;
import com.socialportal.portal.repository.UserEntityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdministrationServiceImplTest {

    @Mock
    private IssueRepository issueRepository;
    @Mock
    private UserEntityRepository userEntityRepository;
    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private AdministrationServiceImpl administrationService;

    @Test
    void getAllIssuesByStatusUsesRequestedPage() {
        var issue = new Issue();
        var expectedPage = new PageImpl<>(List.of(issue));
        when(issueRepository.findAllByStatus(eq(true), any(Pageable.class))).thenReturn(expectedPage);

        var result = administrationService.getAllIssuesByStatus(true, 2, 5);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());

        var captor = ArgumentCaptor.forClass(Pageable.class);
        verify(issueRepository).findAllByStatus(eq(true), captor.capture());
        assertEquals(2, captor.getValue().getPageNumber());
        assertEquals(5, captor.getValue().getPageSize());
    }

    @Test
    void deactivateIssuesByIdArchivesAndSavesIssue() {
        var issue = new Issue();
        issue.setArchived(false);
        when(issueRepository.findById(10L)).thenReturn(Optional.of(issue));

        administrationService.deactivateIssuesById(10L);

        assertTrue(issue.isArchived());
        verify(issueRepository).save(issue);
    }

    @Test
    void deactivateIssuesByIdThrowsWhenIssueMissing() {
        when(issueRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(NoIssueFoundException.class, () ->
                administrationService.deactivateIssuesById(10L));

        verify(issueRepository, never()).save(any());
    }

    @Test
    void createAdminAssignsAdminRoleAndSavesUser() {
        var user = new UserEntity();
        var adminRole = new Roles();
        adminRole.setName("ADMIN");

        when(userEntityRepository.findById(7L)).thenReturn(Optional.of(user));
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(adminRole));

        administrationService.createAdmin(7L);

        assertEquals(adminRole, user.getRole());
        verify(userEntityRepository).save(user);
    }

    @Test
    void createAdminThrowsWhenUserMissing() {
        when(userEntityRepository.findById(7L)).thenReturn(Optional.empty());

        assertThrows(NoUserFoundException.class, () ->
                administrationService.createAdmin(7L));

        verify(userEntityRepository, never()).save(any());
    }

    @Test
    void createAdminThrowsWhenRoleMissing() {
        var user = new UserEntity();
        when(userEntityRepository.findById(7L)).thenReturn(Optional.of(user));
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.empty());

        assertThrows(NoRolesDataBase.class, () ->
                administrationService.createAdmin(7L));

        verify(userEntityRepository, never()).save(any());
    }
}