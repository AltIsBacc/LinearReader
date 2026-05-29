package com.bugfunbug.linearreader.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class LinearCommand {

    private LinearCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LinearCommandRegistrar.register(dispatcher, LinearCommand::hasOperatorPermission);
    }

    private static boolean hasOperatorPermission(CommandSourceStack source) {
        try {
            Method legacyHasPermission = CommandSourceStack.class.getMethod("hasPermission", int.class);
            return (Boolean) legacyHasPermission.invoke(source, 2);
        } catch (NoSuchMethodException e) {
            return hasPermissionSetOperatorPermission(source);
        } catch (IllegalAccessException | InvocationTargetException e) {
            return false;
        }
    }

    private static boolean hasPermissionSetOperatorPermission(CommandSourceStack source) {
        try {
            Method permissionsMethod = CommandSourceStack.class.getMethod("permissions");
            Object permissionSet = permissionsMethod.invoke(source);

            Class<?> permissionsClass = Class.forName("net.minecraft.server.permissions.Permissions");
            Field gamemasterField = permissionsClass.getField("COMMANDS_GAMEMASTER");
            Object gamemasterPermission = gamemasterField.get(null);

            Class<?> permissionClass = Class.forName("net.minecraft.server.permissions.Permission");
            Class<?> permissionSetClass = Class.forName("net.minecraft.server.permissions.PermissionSet");
            Method hasPermission = permissionSetClass.getMethod("hasPermission", permissionClass);
            return (Boolean) hasPermission.invoke(permissionSet, gamemasterPermission);
        } catch (ClassNotFoundException | NoSuchMethodException | NoSuchFieldException |
                 IllegalAccessException | InvocationTargetException e) {
            return false;
        }
    }
}
