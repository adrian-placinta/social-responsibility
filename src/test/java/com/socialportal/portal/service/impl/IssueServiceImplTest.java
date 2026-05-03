package com.socialportal.portal.service.impl;

import com.socialportal.portal.dto.IssueResponseDto;
import com.socialportal.portal.dto.IssueVotesDto;
import com.socialportal.portal.model.geo.IssueLocation;
import com.socialportal.portal.model.geo.UserLocation;
import com.socialportal.portal.model.image.ImageData;
import com.socialportal.portal.model.issues.Issue;
import com.socialportal.portal.model.issues.IssueImage;
import com.socialportal.portal.model.user.UserEntity;
import com.socialportal.portal.pojo.request.IssueRequest;
import com.socialportal.portal.repository.IssueLocationRepository;
import com.socialportal.portal.repository.IssueRepository;
import com.socialportal.portal.repository.UserEntityRepository;
import com.socialportal.portal.service.ImageService;
import com.socialportal.portal.service.VoteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IssueServiceImplTest {

    @Mock private IssueRepository issueRepository;
    @Mock private IssueLocationRepository issueLocationRepository;
    @Mock private UserEntityRepository userEntityRepository;
    @Mock private VoteService voteService;
    @Mock private ImageService imageService;

    private IssueServiceImpl issueService;
    private Authentication authentication;

    private UserEntity testUser;
    private UserLocation testUserLocation;
    private Issue testIssue;
    private IssueLocation testIssueLocation;

    @BeforeEach
    void setUp() {
        issueService = new IssueServiceImpl(
                issueRepository,
                issueLocationRepository,
                userEntityRepository,
                voteService,
                imageService
        );
        authentication = new UsernamePasswordAuthenticationToken("test_user", "secret");

        testUserLocation = new UserLocation();
        testUserLocation.setLatitude(44.4268);
        testUserLocation.setLongitude(26.1025);
        testUserLocation.setRadiusOfInterest(5.0);

        testUser = new UserEntity();
        testUser.setId(1L);
        testUser.setUsername("test_user");
        testUser.setUserLocation(testUserLocation);

        testIssue = new Issue();
        testIssue.setId(100L);
        testIssue.setTitle("Groapa strada");
        testIssue.setDescription("Descriere");
        IssueImage issueImage = new IssueImage();
        testIssue.setImages(List.of(issueImage));

        testIssueLocation = new IssueLocation();
        testIssueLocation.setIssue(testIssue);
    }

    @Test
    void saveSuccessWithImages() throws IOException {
        Issue issueData = new Issue();
        IssueLocation locationData = new IssueLocation();
        IssueRequest request = new IssueRequest();
        request.setIssue(issueData);
        request.setIssueLocation(locationData);

        MultipartFile file = mock(MultipartFile.class);
        List<MultipartFile> files = List.of(file);

        when(userEntityRepository.findByUsername("test_user")).thenReturn(Optional.of(testUser));
        when(imageService.buildImage(any())).thenReturn(new ImageData());
        when(issueRepository.save(any(Issue.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Issue result = issueService.save(request, files, authentication);

        assertNotNull(result);
        assertEquals(testUser, result.getAuthor());
        assertEquals(locationData, result.getIssueLocation());
        verify(imageService).imageMapper(any(ImageData.class), any(IssueImage.class));
        verify(issueRepository).save(any(Issue.class));
    }

    @Test
    void getIssuesReturnsPaginatedResponse() {
        when(userEntityRepository.findByUsername("test_user")).thenReturn(Optional.of(testUser));
        when(issueLocationRepository.findAllByLatitudeBetweenAndLongitudeBetween(
                anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of(testIssueLocation));
        when(voteService.getVotesByIssueId(100L)).thenReturn(new IssueVotesDto(10L, 2L));
        when(voteService.getTotalVotesForUserAndIssue(100L, 1L)).thenReturn(1L);
        when(imageService.getPayload(any(IssueImage.class))).thenReturn(new byte[]{1, 2, 3});

        Page<IssueResponseDto> result = issueService.getIssues(authentication, 0, 10);

        assertNotNull(result);
        assertFalse(result.getContent().isEmpty());
        assertEquals("Groapa strada", result.getContent().get(0).getTitle());
        assertEquals(1L, result.getContent().get(0).getUserVoteSelection());
    }

    @Test
    void getIssuesUserNotFoundThrowsException() {
        authentication = new UsernamePasswordAuthenticationToken("ghost", "secret");
        when(userEntityRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () ->
                issueService.getIssues(authentication, 0, 10));
    }

    @Test
    void saveImageProcessingThrowsRuntimeExceptionOnIoError() throws IOException {
        Issue issueData = new Issue();
        IssueRequest request = new IssueRequest();
        request.setIssue(issueData);

        MultipartFile file = mock(MultipartFile.class);

        when(imageService.buildImage(any())).thenThrow(new IOException("Disk full"));

        assertThrows(RuntimeException.class, () ->
                issueService.save(request, List.of(file), authentication));
    }
}
