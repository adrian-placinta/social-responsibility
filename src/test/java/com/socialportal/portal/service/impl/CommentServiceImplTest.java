package com.socialportal.portal.service.impl;

import com.socialportal.portal.dto.CommentDto;
import com.socialportal.portal.exception.issue.NoCommentFoundException;
import com.socialportal.portal.exception.issue.NoIssueFoundException;
import com.socialportal.portal.model.issues.Comment;
import com.socialportal.portal.model.issues.Issue;
import com.socialportal.portal.model.user.UserEntity;
import com.socialportal.portal.repository.CommentRepository;
import com.socialportal.portal.repository.IssueRepository;
import com.socialportal.portal.repository.UserEntityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock private IssueRepository issueRepository;
    @Mock private UserEntityRepository userEntityRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private Authentication authentication;

    @InjectMocks private CommentServiceImpl commentService;

    private UserEntity testUser;
    private Comment testComment;
    private Issue testIssue;

    @BeforeEach
    void setUp() {
        testUser = new UserEntity();
        testUser.setId(1L);
        testUser.setUsername("test_user");

        testIssue = new Issue();
        testIssue.setId(10L);

        testComment = new Comment();
        testComment.setId(100L);
        testComment.setContent("Comentariu test");
        testComment.setUserEntity(testUser);
        testComment.setIssue(testIssue);
    }

    @Test
    void getComments_Success() {
        when(commentRepository.findAllCommentsForIssue(10L))
                .thenReturn(Optional.of(List.of(testComment)));

        Page<CommentDto> result = commentService.getComments(10L, 0, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("test_user", result.getContent().get(0).getAuthor());
        verify(commentRepository).findAllCommentsForIssue(10L);
    }

    @Test
    void getComments_NotFound() {
        when(commentRepository.findAllCommentsForIssue(anyLong())).thenReturn(Optional.empty());

        assertThrows(NoCommentFoundException.class, () ->
                commentService.getComments(10L, 0, 10));
    }

    @Test
    void addComment_Success() {
        when(authentication.getName()).thenReturn("test_user");
        when(userEntityRepository.findByUsername("test_user")).thenReturn(Optional.of(testUser));
        when(issueRepository.findById(10L)).thenReturn(Optional.of(testIssue));

        CommentDto result = commentService.addComment(authentication, testComment, 10L);

        assertNotNull(result);
        verify(commentRepository).save(testComment);
        assertEquals(testIssue, testComment.getIssue());
    }

    @Test
    void addComment_UserNotFound() {
        when(authentication.getName()).thenReturn("ghost_user");
        when(userEntityRepository.findByUsername("ghost_user")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () ->
                commentService.addComment(authentication, testComment, 10L));
    }

    @Test
    void addComment_IssueNotFound() {
        when(authentication.getName()).thenReturn("test_user");
        when(userEntityRepository.findByUsername("test_user")).thenReturn(Optional.of(testUser));
        when(issueRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(NoIssueFoundException.class, () ->
                commentService.addComment(authentication, testComment, 99L));
    }

    @Test
    void deleteComment_AdminSuccess() {
        doReturn(List.of(new SimpleGrantedAuthority("ADMIN")))
                .when(authentication).getAuthorities();
        when(commentRepository.findById(100L)).thenReturn(Optional.of(testComment));

        commentService.deleteComment(authentication, 100L);

        verify(commentRepository).deleteById(100L);
    }

    @Test
    void deleteComment_UserSuccess() {
        doReturn(List.of(new SimpleGrantedAuthority("USER")))
                .when(authentication).getAuthorities();
        when(authentication.getName()).thenReturn("test_user");
        when(userEntityRepository.findByUsername("test_user")).thenReturn(Optional.of(testUser));

        when(commentRepository.findCommentByUserIdAndCommentId(100L, 1L))
                .thenReturn(Optional.of(testComment));
        when(commentRepository.findById(100L)).thenReturn(Optional.of(testComment));

        commentService.deleteComment(authentication, 100L);

        verify(commentRepository).deleteById(100L);
    }

    @Test
    void deleteComment_AdminCommentNotFound() {
        doReturn(List.of(new SimpleGrantedAuthority("ADMIN")))
                .when(authentication).getAuthorities();
        when(commentRepository.findById(100L)).thenReturn(Optional.empty());

        assertThrows(NoCommentFoundException.class, () ->
                commentService.deleteComment(authentication, 100L));
    }

    @Test
    void deleteComment_UserNotOwner() {
        doReturn(List.of(new SimpleGrantedAuthority("USER")))
                .when(authentication).getAuthorities();
        when(authentication.getName()).thenReturn("test_user");
        when(userEntityRepository.findByUsername("test_user")).thenReturn(Optional.of(testUser));
        when(commentRepository.findCommentByUserIdAndCommentId(100L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(NoCommentFoundException.class, () ->
                commentService.deleteComment(authentication, 100L));
    }

    @Test
    void deleteComment_InvalidRole() {
        when(authentication.getAuthorities()).thenReturn(Collections.emptyList());

        assertThrows(RuntimeException.class, () ->
                commentService.deleteComment(authentication, 100L));
    }
}