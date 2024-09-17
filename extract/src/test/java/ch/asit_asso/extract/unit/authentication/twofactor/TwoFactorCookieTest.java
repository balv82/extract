package ch.asit_asso.extract.unit.authentication.twofactor;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import javax.servlet.http.Cookie;
import ch.asit_asso.extract.authentication.twofactor.TwoFactorCookie;
import ch.asit_asso.extract.domain.User;
import ch.asit_asso.extract.unit.MockEnabledTest;
import ch.asit_asso.extract.utils.Secrets;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.http.ResponseCookie;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

class TwoFactorCookieTest extends MockEnabledTest {

    private static final String COOKIE_NAME_PREFIX = "2FA_ID";

    private static final String NON_TWO_FACTOR_COOKIE_NAME = "JSESSIONID";

    private static final String OTHER_USER_LOGIN = "anotherUser";

    private static final String TEST_USER_LOGIN = "testUser";

    private static final int TOKEN_LENGTH = 64;

    public static final int COOKIE_LIFE_DAYS = 30;

    public static final String EXPECTED_COOKIE_PATH = "/extract-dev";

    @Mock
    private Secrets secrets;

    private String token;

    private User user;

    @BeforeEach
    public void setUp() {
        this.token = RandomStringUtils.randomAlphanumeric(TwoFactorCookieTest.TOKEN_LENGTH);

        this.user = new User(1);
        this.user.setLogin(TwoFactorCookieTest.TEST_USER_LOGIN);

        Mockito.when(this.secrets.hash(anyString())).thenAnswer(
                (Answer<String>) invocationOnMock -> invocationOnMock.getArgument(0)
        );

        Mockito.when(this.secrets.check(anyString(), anyString())).thenAnswer(
                (Answer<Boolean>) invocationOnMock -> Objects.equals(invocationOnMock.getArgument(0),
                                                                     invocationOnMock.getArgument(1))
        );
    }



    @Test
    @DisplayName("Is 2FA cookie generated by the application recognized as such")
    void isGeneratedCookieTwoFactorCookie() {
        TwoFactorCookie twoFactorCookie = new TwoFactorCookie(this.user, this.token, this.secrets,
                                                              TwoFactorCookieTest.EXPECTED_COOKIE_PATH);
        Cookie cookie = twoFactorCookie.toCookie();

        boolean is2faCookie = TwoFactorCookie.isTwoFactorCookie(cookie);

        assertTrue(is2faCookie);
    }



    @Test
    @DisplayName("Is a 2FA cookie created from scratch recognized as such (for compatibility)")
    void isCreatedCookieTwoFactorCookie() {
        Cookie cookie = new Cookie(String.format("%s_%s", TwoFactorCookieTest.COOKIE_NAME_PREFIX, this.user.getLogin()),
                                   this.token);

        boolean is2faCookie = TwoFactorCookie.isTwoFactorCookie(cookie);

        assertTrue(is2faCookie);
    }



    @Test
    @DisplayName("Is a non 2FA cookie recognized as such")
    void isInvalidCookieTwoFactorCookie() {
        Cookie cookie = new Cookie(TwoFactorCookieTest.NON_TWO_FACTOR_COOKIE_NAME, this.token);

        boolean is2faCookie = TwoFactorCookie.isTwoFactorCookie(cookie);

        assertFalse(is2faCookie);
    }



    @Test
    @DisplayName("Instantiates object from cookie generated by the application")
    void fromGeneratedCookie() {
        TwoFactorCookie twoFactorCookie = new TwoFactorCookie(this.user, this.token, this.secrets,
                                                              TwoFactorCookieTest.EXPECTED_COOKIE_PATH);
        Cookie cookie = twoFactorCookie.toCookie();

        AtomicReference<TwoFactorCookie> result = new AtomicReference<>(null);

        assertDoesNotThrow(() -> {
            result.set(TwoFactorCookie.fromCookie(cookie, this.secrets));
        });

        assertNotNull(result);
    }



    @Test
    @DisplayName("Instantiates object from cookie created from scratch (for compatibility)")
    void fromCreatedCookie() {
        Cookie cookie = new Cookie(String.format("%s_%s", TwoFactorCookieTest.COOKIE_NAME_PREFIX, this.user.getLogin()),
                                   this.token);

        AtomicReference<TwoFactorCookie> result = new AtomicReference<>(null);

        assertDoesNotThrow(() -> {
            result.set(TwoFactorCookie.fromCookie(cookie, this.secrets));
        });

        assertNotNull(result);
    }



    @Test
    @DisplayName("Instantiates object from non-2FA cookie")
    void fromInvalidCookie() {
        Cookie cookie = new Cookie(TwoFactorCookieTest.NON_TWO_FACTOR_COOKIE_NAME, this.token);

        assertThrows(IllegalArgumentException.class, () -> {
            TwoFactorCookie.fromCookie(cookie, this.secrets);
        });
    }



    @Test
    @DisplayName("Checks that token is preserved during instantiation with parameters")
    void getTokenFromParameters() {
        TwoFactorCookie twoFactorCookie = new TwoFactorCookie(this.user, this.token, this.secrets,
                                                              TwoFactorCookieTest.EXPECTED_COOKIE_PATH);

        String cookieToken = twoFactorCookie.getToken();

        assertEquals(this.token, cookieToken);
    }

    @Test
    @DisplayName("Checks that token is preserved during instantiation with cookie")
    void getTokenFromCookie() {
        Cookie cookie = new Cookie(String.format("%s_%s", TwoFactorCookieTest.COOKIE_NAME_PREFIX, this.user.getLogin()),
                                   this.token);
        TwoFactorCookie twoFactorCookie = TwoFactorCookie.fromCookie(cookie, this.secrets);

        String cookieToken = twoFactorCookie.getToken();

        assertEquals(this.token, cookieToken);
    }



    @Test
    @DisplayName("Is the cookie correctly identified as belonging to the user")
    void isCookieUser() {
        String userLogin = this.user.getLogin();
        Cookie cookie = new Cookie(String.format("%s_%s", TwoFactorCookieTest.COOKIE_NAME_PREFIX, userLogin),
                                   this.token);
        TwoFactorCookie twoFactorCookie = TwoFactorCookie.fromCookie(cookie, this.secrets);

        boolean isCookieUser = twoFactorCookie.isCookieUser(this.user);

        assertTrue(isCookieUser);
        Mockito.verify(this.secrets, Mockito.times(1)).check(eq(userLogin), anyString());
    }



    @Test
    @DisplayName("Is the cookie for another user correctly identified as not belonging to the user")
    void isCookieUserWithDifferentCookie() {
        User otherUser = new User(2);
        otherUser.setLogin(TwoFactorCookieTest.OTHER_USER_LOGIN);

        Cookie cookie = new Cookie(String.format("%s_%s", TwoFactorCookieTest.COOKIE_NAME_PREFIX, otherUser.getLogin()),
                                   this.token);
        TwoFactorCookie twoFactorCookie = TwoFactorCookie.fromCookie(cookie, this.secrets);

        boolean isCookieUser = twoFactorCookie.isCookieUser(this.user);

        assertFalse(isCookieUser);
        Mockito.verify(this.secrets, Mockito.times(1)).check(eq(this.user.getLogin()), anyString());
    }



    @Test
    @DisplayName("Creation of a cookie")
    void toCookie() {
        TwoFactorCookie twoFactorCookie = new TwoFactorCookie(this.user, this.token, this.secrets,
                                                              TwoFactorCookieTest.EXPECTED_COOKIE_PATH);

        Cookie cookie = twoFactorCookie.toCookie(false);

        assertNotNull(cookie);
        // The secrets hash method is stubbed so we can compare directly the value
        assertEquals(String.format("%s_%s", TwoFactorCookieTest.COOKIE_NAME_PREFIX, this.user.getLogin()),
                     cookie.getName());
        // We just need to check that the method has been called
        Mockito.verify(this.secrets, Mockito.times(1)).hash(this.user.getLogin());

        assertEquals(this.token, cookie.getValue());
        assertEquals(TwoFactorCookieTest.EXPECTED_COOKIE_PATH, cookie.getPath());
        assertTrue(cookie.isHttpOnly());
        assertEquals(TwoFactorCookieTest.COOKIE_LIFE_DAYS, Duration.ofSeconds(cookie.getMaxAge()).toDays());
    }



    @Test
    @DisplayName("Creation of an expiring cookie")
    void toExpiringCookie() {
        TwoFactorCookie twoFactorCookie = new TwoFactorCookie(this.user, this.token, this.secrets,
                                                              TwoFactorCookieTest.EXPECTED_COOKIE_PATH);

        Cookie cookie = twoFactorCookie.toCookie(true);

        assertNotNull(cookie);
        // The secrets hash method is stubbed so we can compare directly the value
        assertEquals(String.format("%s_%s", TwoFactorCookieTest.COOKIE_NAME_PREFIX, this.user.getLogin()),
                     cookie.getName());
        // We just need to check that the method has been called
        Mockito.verify(this.secrets, Mockito.times(1)).hash(this.user.getLogin());

        assertNull(cookie.getValue());
        assertEquals(TwoFactorCookieTest.EXPECTED_COOKIE_PATH, cookie.getPath());
        assertTrue(cookie.isHttpOnly());
        assertEquals(0, cookie.getMaxAge());
    }



    @Test
    @DisplayName("Creation of a response cookie")
    void toResponseCookie() {
        TwoFactorCookie twoFactorCookie = new TwoFactorCookie(this.user, this.token, this.secrets,
                                                              TwoFactorCookieTest.EXPECTED_COOKIE_PATH);

        ResponseCookie cookie = twoFactorCookie.toResponseCookie(false);

        assertNotNull(cookie);
        // The secrets hash method is stubbed so we can compare directly the value
        assertEquals(String.format("%s_%s", TwoFactorCookieTest.COOKIE_NAME_PREFIX, this.user.getLogin()),
                     cookie.getName());
        // We just need to check that the method has been called
        Mockito.verify(this.secrets, Mockito.times(1)).hash(this.user.getLogin());

        assertEquals(this.token, cookie.getValue());
        assertEquals(TwoFactorCookieTest.EXPECTED_COOKIE_PATH, cookie.getPath());
        assertTrue(cookie.isHttpOnly());
        assertEquals(Duration.ofDays(TwoFactorCookieTest.COOKIE_LIFE_DAYS), cookie.getMaxAge());
    }



    @Test
    @DisplayName("Creation of an expiring response cookie")
    void toExpiringResponseCookie() {
        TwoFactorCookie twoFactorCookie = new TwoFactorCookie(this.user, this.token, this.secrets,
                                                              TwoFactorCookieTest.EXPECTED_COOKIE_PATH);

        ResponseCookie cookie = twoFactorCookie.toResponseCookie(true);

        assertNotNull(cookie);
        // The secrets hash method is stubbed so we can compare directly the value
        assertEquals(String.format("%s_%s", TwoFactorCookieTest.COOKIE_NAME_PREFIX, this.user.getLogin()),
                     cookie.getName());
        // We just need to check that the method has been called
        Mockito.verify(this.secrets, Mockito.times(1)).hash(this.user.getLogin());

        assertEquals("", cookie.getValue());
        assertEquals(TwoFactorCookieTest.EXPECTED_COOKIE_PATH, cookie.getPath());
        assertTrue(cookie.isHttpOnly());
        assertEquals(Duration.ofDays(0), cookie.getMaxAge());
    }
}
