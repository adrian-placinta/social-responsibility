package com.socialportal.portal.service.impl;

import com.socialportal.portal.dto.IssueVotesDto;
import com.socialportal.portal.exception.issue.NoIssueFoundException;
import com.socialportal.portal.model.issues.Issue;
import com.socialportal.portal.model.issues.IssueVote;
import com.socialportal.portal.model.user.UserEntity;
import com.socialportal.portal.repository.IssueRepository;
import com.socialportal.portal.repository.UserEntityRepository;
import com.socialportal.portal.repository.VoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoteServiceImplTest {

    @Mock
    private VoteRepository voteRepository;
    @Mock
    private UserEntityRepository userEntityRepository;
    @Mock
    private IssueRepository issueRepository;

    private VoteServiceImpl voteService;

    @BeforeEach
    void setUp() {
        voteService = new VoteServiceImpl(voteRepository, userEntityRepository, issueRepository);
    }

    @Test
    void getVotesByIssueIdReturnsCountsWithDefaults() {
        when(voteRepository.countVotes((byte) 1, 6L)).thenReturn(Optional.of(4L));
        when(voteRepository.countVotes((byte) -1, 6L)).thenReturn(Optional.empty());

        IssueVotesDto result = voteService.getVotesByIssueId(6L);

        assertEquals(4L, result.getUpVotes());
        assertEquals(0L, result.getDownVotes());
    }

    @Test
    void voteUpdatesExistingVoteWhenValueChanges() {
        var auth = new TestingAuthenticationToken("alice", null);
        var user = new UserEntity();
        user.setId(5L);
        var issue = new Issue();
        var vote = new IssueVote(1L, -1, user, issue);
        when(userEntityRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(issueRepository.findById(7L)).thenReturn(Optional.of(issue));
        when(voteRepository.findByUserId(5L)).thenReturn(Optional.of(vote));
        when(voteRepository.countVotes((byte) 1, 7L)).thenReturn(Optional.of(1L));
        when(voteRepository.countVotes((byte) -1, 7L)).thenReturn(Optional.of(0L));

        Integer result = voteService.vote(auth, 99, 7L);

        assertEquals(1, result);
        assertEquals(1, vote.getVoteValue());
        verify(voteRepository).save(vote);
    }

    @Test
    void voteCreatesNewVoteAndArchivesIssueWhenThresholdReached() {
        var auth = new TestingAuthenticationToken("alice", null);
        var user = new UserEntity();
        user.setId(5L);
        var issue = new Issue();
        issue.setArchived(false);
        when(userEntityRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(issueRepository.findById(7L)).thenReturn(Optional.of(issue));
        when(voteRepository.findByUserId(5L)).thenReturn(Optional.empty());
        when(voteRepository.countVotes((byte) 1, 7L)).thenReturn(Optional.of(6L));
        when(voteRepository.countVotes((byte) -1, 7L)).thenReturn(Optional.of(12L));

        Integer result = voteService.vote(auth, -2, 7L);

        assertEquals(-1, result);
        assertTrue(issue.isArchived());
        verify(voteRepository).save(any(IssueVote.class));
    }

    @Test
    void voteThrowsWhenUserMissing() {
        var auth = new TestingAuthenticationToken("alice", null);
        when(userEntityRepository.findByUsername("alice")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> voteService.vote(auth, 1, 7L));
    }

    @Test
    void voteThrowsWhenIssueMissing() {
        var auth = new TestingAuthenticationToken("alice", null);
        var user = new UserEntity();
        user.setId(5L);
        when(userEntityRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(issueRepository.findById(7L)).thenReturn(Optional.empty());

        assertThrows(NoIssueFoundException.class, () -> voteService.vote(auth, 1, 7L));
    }

    @Test
    void getTotalVotesForUserAndIssueReturnsZeroForAnonymousUserId() {
        assertEquals(0L, voteService.getTotalVotesForUserAndIssue(0L, 7L));
        verifyNoInteractions(voteRepository);
    }

    @Test
    void getTotalVotesForUserAndIssueDelegatesToRepository() {
        when(voteRepository.getTotalVotesForUserAndIssue(5L, 7L)).thenReturn(Optional.of(1L));

        assertEquals(1L, voteService.getTotalVotesForUserAndIssue(5L, 7L));
    }
}
