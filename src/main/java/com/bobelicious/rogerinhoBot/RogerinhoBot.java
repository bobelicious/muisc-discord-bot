package com.bobelicious.rogerinhoBot;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;

import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.voice.AudioProvider;

public class RogerinhoBot {

    public static void main(String[] args) {

        final AudioPlayerManager playerManager = new DefaultAudioPlayerManager();

        playerManager.getConfiguration()
                .setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);

        AudioSourceManagers.registerRemoteSources(playerManager);

        final AudioPlayer player = playerManager.createPlayer();

        AudioProvider provider = new LavaPlayerAudioProvider(player);

        commands.put("join", event -> {
            final Member member = event.getMember().orElse(null);
            if (member != null) {
                final VoiceState voiceState = member.getVoiceState().block();
                if (voiceState != null) {
                    final VoiceChannel channel = voiceState.getChannel().block();
                    if (channel != null) {
                        channel.join(spec -> spec.setProvider(provider)).block();
                    }
                }
            }
        });

        final TrackScheduler scheduler = new TrackScheduler(player);

        commands.put("play", event -> {
            final Member member = event.getMember().orElse(null);
            if (member != null) {
                final VoiceState voiceState = member.getVoiceState().block();
                if (voiceState != null) {
                    final VoiceChannel channel = voiceState.getChannel().block();
                    if (channel != null) {
                        channel.join(spec -> spec.setProvider(provider)).block();
                        final String content = event.getMessage().getContent();
                        final List<String> command = Arrays.asList(content.split(" "));
                        playerManager.loadItem(command.get(1), scheduler);
                    }
                }
            }
        });

        commands.put("exit", event -> {
            final Member member = event.getMember().orElse(null);
            if (member != null) {
                final VoiceState voiceState = member.getVoiceState().block();
                if (voiceState != null) {
                    final VoiceChannel channel = voiceState.getChannel().block();
                    if (channel != null) {
                        channel.sendDisconnectVoiceState().block();
                    }
                }
            }
        });

        commands.put("pause", event -> {
            player.setPaused(!player.isPaused());
            if (player.isPaused())
            event.getMessage().getChannel().block().createMessage("Pausei a musica caraio!!").block();
            else
            event.getMessage().getChannel().block().createMessage("Já voltei a musica o cuzão").block();
        });

        final GatewayDiscordClient client = DiscordClientBuilder.create(args[0]).build()
                .login()
                .block();

        client.getEventDispatcher().on(MessageCreateEvent.class)
                // subscribe is like block, in that it will *request* for action
                // to be done, but instead of blocking the thread, waiting for it
                // to finish, it will just execute the results asynchronously.
                .subscribe(event -> {
                    // 3.1 Message.getContent() is a String
                    final String content = event.getMessage().getContent();

                    for (final Map.Entry<String, Command> entry : commands.entrySet()) {
                        // We will be using ! as our "prefix" to any command in the system.
                        if (content.startsWith('*' + entry.getKey())) {
                            entry.getValue().execute(event);
                            break;
                        }
                    }
                });

        client.onDisconnect().block();
    }

    private static final Map<String, Command> commands = new HashMap<>();

    static {
        commands.put("ping", event -> event.getMessage()
                .getChannel().block()
                .createMessage("Pong!").block());
    }

}
