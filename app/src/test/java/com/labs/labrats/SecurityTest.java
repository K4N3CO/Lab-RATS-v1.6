package com.labs.labrats;

import android.content.Context;

import com.labs.labrats.router.PasswordHasher;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class SecurityTest {

    private Context context;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
    }

    @Test
    public void testPasswordHasherVerificationAndMigration() {
        // Initial state: Default plain text password "admin1337"
        boolean validDefault = PasswordHasher.verifyPassword(context, "admin1337");
        Assert.assertTrue(validDefault);

        // After initial login, password is automatically migrated to PBKDF2 hash
        boolean validAfterMigration = PasswordHasher.verifyPassword(context, "admin1337");
        Assert.assertTrue(validAfterMigration);

        // Wrong password should fail
        boolean invalidPass = PasswordHasher.verifyPassword(context, "wrongpass123");
        Assert.assertFalse(invalidPass);
    }

    @Test
    public void testPasswordHasherSaveNewPassword() {
        PasswordHasher.savePassword(context, "NewTacticalKey2026");

        boolean validNew = PasswordHasher.verifyPassword(context, "NewTacticalKey2026");
        Assert.assertTrue(validNew);

        boolean invalidOld = PasswordHasher.verifyPassword(context, "OldPassword123");
        Assert.assertFalse(invalidOld);
    }

    @Test
    public void testSystemAnalyticsEncryptionDecryption() {
        String testData = "CONFIDENTIAL_TELEMETRY_LOG_ENTRY_1337";
        byte[] encrypted = SystemAnalytics.encrypt(testData);

        Assert.assertNotNull(encrypted);
        Assert.assertTrue(encrypted.length > 0);

        String decrypted = SystemAnalytics.decrypt(encrypted);
        Assert.assertEquals(testData, decrypted);
    }
}
