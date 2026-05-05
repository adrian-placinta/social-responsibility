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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IssueServiceImplTest {

    @Mock
    private IssueRepository issueRepository;
    @Mock
    private IssueLocationRepository issueLocationRepository;
    @Mock
    private UserEntityRepository userEntityRepository;
    @Mock
    private VoteService voteService;
    @Mock
    private ImageService imageService;

    @InjectMocks
    private IssueServiceImpl issueService;

    private Authentication authentication;
    private UserEntity testUser;
    private IssueLocation testIssueLocation;

    @BeforeEach
    void setUp() {
        authentication = new UsernamePasswordAuthenticationToken("test_user", "secret");

        UserLocation testUserLocation = new UserLocation();
        testUserLocation.setLatitude(44.4268);
        testUserLocation.setLongitude(26.1025);
        testUserLocation.setRadiusOfInterest(5.0);

        testUser = new UserEntity();
        testUser.setId(1L);
        testUser.setUsername("test_user");
        testUser.setUserLocation(testUserLocation);

        Issue testIssue = new Issue();
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
        doAnswer(invocation -> {
            IssueImage target = invocation.getArgument(1);
            target.setName("mapped-image");
            return null;
        }).when(imageService).imageMapper(any(ImageData.class), any(IssueImage.class));
        when(issueRepository.save(any(Issue.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Issue result = issueService.save(request, files, authentication);

        assertNotNull(result);
        assertEquals(testUser, result.getAuthor());
        assertEquals(locationData, result.getIssueLocation());
        assertEquals(1, result.getImages().size());
        assertNotNull(result.getImages().get(0));
        assertEquals("mapped-image", result.getImages().get(0).getName());
        verify(imageService).imageMapper(any(ImageData.class), any(IssueImage.class));
        verify(issueRepository).save(any(Issue.class));
    }

    @Test
    void getIssuesReturnsPaginatedResponseAndUsesExpectedBoundingBox() {
        when(userEntityRepository.findByUsername("test_user")).thenReturn(Optional.of(testUser));
        ArgumentCaptor<Double> minLatCaptor = ArgumentCaptor.forClass(Double.class);
        ArgumentCaptor<Double> maxLatCaptor = ArgumentCaptor.forClass(Double.class);
        ArgumentCaptor<Double> minLongCaptor = ArgumentCaptor.forClass(Double.class);
        ArgumentCaptor<Double> maxLongCaptor = ArgumentCaptor.forClass(Double.class);
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
        assertEquals("Descriere", result.getContent().get(0).getDescription());
        assertEquals(1L, result.getContent().get(0).getUserVoteSelection());
        assertArrayEquals(new byte[]{1, 2, 3}, result.getContent().get(0).getImages().get(0));
        assertEquals(10L, result.getContent().get(0).getIssueVoteStats().getUpVotes());
        assertEquals(2L, result.getContent().get(0).getIssueVoteStats().getDownVotes());
        assertEquals(1, result.getTotalElements());

        verify(issueLocationRepository).findAllByLatitudeBetweenAndLongitudeBetween(
                minLatCaptor.capture(),
                maxLatCaptor.capture(),
                minLongCaptor.capture(),
                maxLongCaptor.capture()
        );

        double latitude = 44.4268;
        double longitude = 26.1025;
        double radiusMeters = 5_000.0;
        double latRadian = Math.toRadians(latitude);
        double degLatKm = 110.574235;
        double degLongKm = 110.572833 * Math.cos(latRadian);
        double deltaLat = radiusMeters / 1000.0 / degLatKm;
        double deltaLong = radiusMeters / 1000.0 / degLongKm;

        assertEquals(latitude - deltaLat, minLatCaptor.getValue(), 1.0e-9);
        assertEquals(latitude + deltaLat, maxLatCaptor.getValue(), 1.0e-9);
        assertEquals(longitude - deltaLong, minLongCaptor.getValue(), 1.0e-9);
        assertEquals(longitude + deltaLong, maxLongCaptor.getValue(), 1.0e-9);
    }

    @Test
    void getIssuesUserNotFoundThrowsException() {
        Authentication ghostAuth = new UsernamePasswordAuthenticationToken("ghost", "secret");
        when(userEntityRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () ->
                issueService.getIssues(ghostAuth, 0, 10));
    }

    @Test
    void saveImageProcessingThrowsRuntimeExceptionOnIoError() throws IOException {
        IssueRequest request = new IssueRequest();
        request.setIssue(new Issue());
        MultipartFile file = mock(MultipartFile.class);

        when(imageService.buildImage(any())).thenThrow(new IOException("Disk full"));

        assertThrows(RuntimeException.class, () ->
                issueService.save(request, List.of(file), authentication));
    }

    @Test
    void getIssuesUsesRadiusInKilometersConvertedToMeters() {
        UserLocation customUserLocation = new UserLocation();
        customUserLocation.setLatitude(10.0);
        customUserLocation.setLongitude(20.0);
        customUserLocation.setRadiusOfInterest(1.5);
        testUser.setUserLocation(customUserLocation);

        when(userEntityRepository.findByUsername("test_user")).thenReturn(Optional.of(testUser));
        when(issueLocationRepository.findAllByLatitudeBetweenAndLongitudeBetween(
                anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of());

        Page<IssueResponseDto> result = issueService.getIssues(authentication, 0, 10);

        assertTrue(result.getContent().isEmpty());

        ArgumentCaptor<Double> minLatCaptor = ArgumentCaptor.forClass(Double.class);
        ArgumentCaptor<Double> maxLatCaptor = ArgumentCaptor.forClass(Double.class);
        ArgumentCaptor<Double> minLongCaptor = ArgumentCaptor.forClass(Double.class);
        ArgumentCaptor<Double> maxLongCaptor = ArgumentCaptor.forClass(Double.class);
        verify(issueLocationRepository).findAllByLatitudeBetweenAndLongitudeBetween(
                minLatCaptor.capture(),
                maxLatCaptor.capture(),
                minLongCaptor.capture(),
                maxLongCaptor.capture()
        );

        double latRadian = Math.toRadians(10.0);
        double degLatKm = 110.574235;
        double degLongKm = 110.572833 * Math.cos(latRadian);
        double deltaLat = 1.5 / degLatKm;
        double deltaLong = 1.5 / degLongKm;

        assertEquals(10.0 - deltaLat, minLatCaptor.getValue(), 1.0e-9);
        assertEquals(10.0 + deltaLat, maxLatCaptor.getValue(), 1.0e-9);
        assertEquals(20.0 - deltaLong, minLongCaptor.getValue(), 1.0e-9);
        assertEquals(20.0 + deltaLong, maxLongCaptor.getValue(), 1.0e-9);
    }
}
