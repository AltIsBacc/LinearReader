package com.bugfunbug.linearreader.command;

import com.bugfunbug.linearreader.LinearTestSupport;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class LinearCommandRegistrarTest {

    @BeforeEach
    void setUp() {
        LinearTestSupport.resetState();
    }

    @Test
    void exposesVoxyCompatPrepareWithoutLegacyAlias() {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        LinearCommandRegistrar.register(dispatcher, source -> true);

        CommandNode<CommandSourceStack> root = dispatcher.getRoot().getChild("linearreader");
        assertNotNull(root);

        CommandNode<CommandSourceStack> voxyCompat = root.getChild("voxy-compat");
        assertNotNull(voxyCompat);
        assertNotNull(voxyCompat.getCommand(), "bare /linearreader voxy-compat should show status");
        assertNotNull(voxyCompat.getChild("prepare"));
        assertNotNull(voxyCompat.getChild("cleanup"));

        assertNull(voxyCompat.getChild("start"));
        assertNull(root.getChild("voxy-mca"));
    }
}
