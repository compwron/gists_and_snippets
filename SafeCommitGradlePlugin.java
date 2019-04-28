package com.example.gradleplugin

import com.google.common.io.Resources
import org.ajoberstar.grgit.Grgit

import java.nio.charset.Charset

class PreCommitHooks {
    public static final String HOOK_PATH = '.git/hooks/'
    public static final String COMMIT_MSG_HOOK_PATH = HOOK_PATH + 'commit-msg'
    public static final String SECURITY_CHECK_HOOK_PATH = HOOK_PATH + 'pre-commit'

    public static final String SAFE_COMMIT_HOOK_GIT_SOURCE = 'https://github.com/jandre/safe-commit-hook.git'
    public static final String SAFE_COMMIT_HOOK_DIR = ".safe-commit-hook" // this is set in the remote codebase itself
    public static final String SAFE_COMMIT_HOOK_CONFIG = '/git-deny-patterns.json'

    public static ensureGitHooks() {
        createHooksDirectory()
        createSafeCommitHook()
        createHookFromResource(COMMIT_MSG_HOOK_PATH, 'commit-msg')
    }

    private static void createSafeCommitHook() {
        def fullConfigFile = System.getProperty('user.home') + '/' + SAFE_COMMIT_HOOK_DIR + SAFE_COMMIT_HOOK_CONFIG
        def configExists = new File(fullConfigFile).exists()
        def securityHookExists = new File(SECURITY_CHECK_HOOK_PATH).exists()
        if (!configExists || !securityHookExists) {
            if (!(new File(SAFE_COMMIT_HOOK_DIR).exists())) {
                Grgit.clone(dir: SAFE_COMMIT_HOOK_DIR, uri: SAFE_COMMIT_HOOK_GIT_SOURCE)
            }
            if (!configExists) {
                printHookInstallMessage(fullConfigFile)
                createSafeCommitHookConfig()
            }
            if (!securityHookExists) {
                printHookInstallMessage(SECURITY_CHECK_HOOK_PATH)
                createSafeConfigHook()
            }
        }
        removeSafeCommitHookRepo()
    }

    private static void removeSafeCommitHookRepo() {
        new File(SAFE_COMMIT_HOOK_DIR).deleteDir();
    }

    private static void createSafeCommitHookConfig() {
        // The commit hook code requires that this be in the user homedir
        def homedir_safe_commit_hook_path = System.getProperty('user.home') + '/' + SAFE_COMMIT_HOOK_DIR
        new File(homedir_safe_commit_hook_path).mkdirs()
        new File(homedir_safe_commit_hook_path + SAFE_COMMIT_HOOK_CONFIG) << new File(SAFE_COMMIT_HOOK_DIR + SAFE_COMMIT_HOOK_CONFIG).text
    }

    private static void createSafeConfigHook() {
        def git_safe_commit_hook = new File(SECURITY_CHECK_HOOK_PATH) << new File(SAFE_COMMIT_HOOK_DIR + '/safe-commit-hook.py').text
        git_safe_commit_hook.setExecutable(true) // Or else git silently fails to run the hook
    }

    private static void createHooksDirectory() {
        new File(HOOK_PATH).mkdirs()
    }

    private static void createHookFromResource(String hookPath, String resourceName) {
        if (missingHook(hookPath)) {
            printHookInstallMessage(hookPath)
            def pre_commit_file = new File(hookPath)
            pre_commit_file.write(Resources.toString(Resources.getResource(resourceName), Charset.defaultCharset()))
            pre_commit_file.setExecutable(true)
        }
    }

    private static void printHookInstallMessage(String hookPath) {
        println("Git hook ${hookPath} not present, installing...")
    }

    private static boolean missingHook(String hookPath) {
        !(new File(hookPath).exists())
    }
}
