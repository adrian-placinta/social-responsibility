package com.socialportal.portal.service.impl;

import com.socialportal.portal.dto.LoginResponse;
import com.socialportal.portal.exception.user.NoRolesDataBase;
import com.socialportal.portal.exception.user.UserAlreadyExists;
import com.socialportal.portal.model.user.Roles;
import com.socialportal.portal.model.user.UserEntity;
import com.socialportal.portal.model.user.UserImage;
import com.socialportal.portal.pojo.request.LoginRequest;
import com.socialportal.portal.pojo.request.SignUpRequest;
import com.socialportal.portal.repository.RoleRepository;
import com.socialportal.portal.repository.UserEntityRepository;
import com.socialportal.portal.security.JwtService;
import com.socialportal.portal.service.ImageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private UserEntityRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private ImageService imageService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void signUp_SuccessfullyRegistersNewUser() throws IOException {
        var user = new UserEntity();
        user.setUsername("new_user");
        user.setPassword("rawPassword");

        var request = new SignUpRequest();
        request.setUserEntity(user);

        var role = new Roles();
        role.setName("USER");

        when(userRepository.existsByUsername("new_user")).thenReturn(false);
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("rawPassword")).thenReturn("hashedPassword");

        var response = userService.signUp(request, null);

        assertEquals("new_user", response.getUsername());
        assertEquals("hashedPassword", user.getPassword());
        assertEquals(role, user.getRole());
        verify(userRepository).save(user);
    }

    @Test
    void signUp_FailsWhenUsernameIsTaken() {
        var user = new UserEntity();
        user.setUsername("existing_user");
        var request = new SignUpRequest();
        request.setUserEntity(user);

        when(userRepository.existsByUsername("existing_user")).thenReturn(true);

        assertThrows(UserAlreadyExists.class, () -> userService.signUp(request, null));
        verify(userRepository, never()).save(any());
    }

    @Test
    void signUp_IncludesProfilePictureWhenProvided() throws IOException {
        var user = new UserEntity();
        var request = new SignUpRequest();
        request.setUserEntity(user);
        var file = mock(MultipartFile.class);

        when(roleRepository.findByName("USER")).thenReturn(Optional.of(new Roles()));
        when(imageService.buildImage(file)).thenReturn(new com.socialportal.portal.model.image.ImageData());

        userService.signUp(request, file);

        assertNotNull(user.getUserImage());
        verify(imageService).imageMapper(any(), any(UserImage.class));
    }

    @Test
    void login_ReturnsValidJwtResponse() {
        var loginRequest = new LoginRequest();
        loginRequest.setUsername("user");
        loginRequest.setPassword("pass");

        when(authenticationManager.authenticate(any())).thenReturn(mock(Authentication.class));
        when(jwtService.generateToken(any())).thenReturn("fake-jwt-token");

        LoginResponse response = userService.login(loginRequest);

        assertAll(
                () -> assertEquals("Bearer", response.getTokenType()),
                () -> assertEquals("fake-jwt-token", response.getAccess())
        );
    }

    @Test
    void getProfilePic_ReturnsEmptyDtoIfNoImageExists() {
        var user = new UserEntity();
        user.setUserImage(null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        var result = userService.getProfilePic(1L);

        assertNull(result.getUserImage());
        verifyNoInteractions(imageService);
    }

    @Test
    void signUp_ThrowsExceptionIfRoleTableIsCorrupt() {
        var request = new SignUpRequest();
        request.setUserEntity(new UserEntity());

        when(roleRepository.findByName("USER")).thenReturn(Optional.empty());

        assertThrows(NoRolesDataBase.class, () -> userService.signUp(request, null));
    }
}