/*
 * Copyright (c) 2011, 2012, Tyler Blair
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ''AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and contributors and should not be interpreted as representing official policies,
 * either expressed or implied, of anybody else.
 */

package com.griefcraft.attribute;

import com.griefcraft.AccessProvider;
import com.griefcraft.Engine;
import com.griefcraft.ProtectionAccess;
import com.griefcraft.command.Command;
import com.griefcraft.command.CommandContext;
import com.griefcraft.command.CommandException;
import com.griefcraft.command.SenderType;
import com.griefcraft.entity.Player;
import com.griefcraft.model.AbstractAttribute;
import com.griefcraft.model.Protection;
import com.griefcraft.util.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

//// TODO : hash pws
public class PasswordAttributeFactory implements ProtectionAttributeFactory<String> {

    /**
     * Players that are permitted to access this protection
     * <ProtectionId, Set<Player>>
     */
    private static final Map<Integer, Set<String>> players = new HashMap<Integer, Set<String>>();

    /**
     * Attribute name
     */
    public static final String NAME = "password";

    /**
     * The engine instance
     */
    private Engine engine;

    public PasswordAttributeFactory(Engine engine) {
        this.engine = engine;

        try {
            engine.getCommandHandler().registerCommands(this);
        } catch (CommandException e) {
            e.printStackTrace();
        }
    }

    public String getName() {
        return NAME;
    }

    public AbstractAttribute<String> createAttribute() {
        return new PasswordAttribute();
    }

    @Command(
            command = "lwc password",
            permission = "lwc.password",
            aliases = {"cpassword"},
            accepts = SenderType.PLAYER,
            min = 1
    )
    public void passwordCommand(CommandContext context) {
        Player player = (Player) context.getCommandSender();
        String password = StringUtils.join(context.getArgumentsArray());

        Object request = player.getAttribute("password_request");

        if (request == null) {
            player.sendMessage("&4You have no pending protection unlock.");
            return;
        }

        PasswordAttribute attribute = (PasswordAttribute) request;

        if (password.equals(attribute.getValue())) {
            int protectionId = (Integer) player.getAttribute("password_protection_id");
            player.sendMessage("&2Granted access to the protection!");
            player.removeAttribute("password_request");
            player.removeAttribute("password_protection_id");

            Set<String> playerList = players.get(protectionId);

            if (playerList == null) {
                playerList = new HashSet<String>();
                players.put(protectionId, playerList);
            }

            playerList.add(player.getName());
        } else {
            player.sendMessage("&4Invalid password.");
        }
    }

    private class PasswordAttribute extends StringAttribute implements AccessProvider {

        public PasswordAttribute() {
            super(engine, NAME, "");
        }

        public ProtectionAccess getAccess(Protection protection, Player player) {
            String playerName = player.getName();

            if (players.containsKey(protection.getId()) && players.get(protection.getId()).contains(playerName)) {
                return ProtectionAccess.MEMBER;
            }

            player.sendMessage("&4This protection is locked by a password.\n&4To enter the password, use: &3/cpassword <password>");
            player.setAttribute("password_request", this);
            player.setAttribute("password_protection_id", protection.getId());
            return ProtectionAccess.EXPLICIT_DENY;
        }

    }

}