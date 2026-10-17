package com.activehub.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** Habilita {@code @Async}: hoy lo usa sólo el envío por correo de las notificaciones. */
@Configuration
@EnableAsync
public class AsyncConfig {
}
