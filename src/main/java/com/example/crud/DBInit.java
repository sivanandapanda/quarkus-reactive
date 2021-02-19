package com.example.crud;


import io.quarkus.runtime.StartupEvent;
import io.vertx.mutiny.pgclient.PgPool;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

@ApplicationScoped
public class DBInit {

    private final PgPool client;
    private final boolean schemaCreate;

    public DBInit(PgPool client, @ConfigProperty(name="myapp.schema.create", defaultValue = "true") boolean schemaCreate) {
        this.client = client;
        this.schemaCreate = schemaCreate;
    }

    void onStart(@Observes StartupEvent event) {
        if(schemaCreate) {
            initDb();
        }
    }

    private void initDb() {
        client.query("DROP TABLE IF EXISTS fruits").execute()
                .flatMap(r -> client.query("CREATE TABLE fruits (id SERIAL PRIMARY KEY, name TEXT NOT NULL)").execute())
                .flatMap(r -> client.query("INSERT INTO fruits (name) values ('Kiwi')").execute())
                .flatMap(r -> client.query("INSERT INTO fruits (name) values ('Durian')").execute())
                .flatMap(r -> client.query("INSERT INTO fruits (name) values ('Pomelo')").execute())
                .flatMap(r -> client.query("INSERT INTO fruits (name) values ('Lychee')").execute())
                .await().indefinitely();
    }
}
