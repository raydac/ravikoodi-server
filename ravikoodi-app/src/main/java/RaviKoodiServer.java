import java.io.PrintStream;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;

@SpringBootApplication
public class RaviKoodiServer {

	public static void main(@NonNull String[] args) {
      new SpringApplicationBuilder(RaviKoodiServer.class)
              .web(WebApplicationType.NONE)
              .headless(false)
              .banner((@NonNull final Environment environment, @NonNull final Class<?> sourceClass, @NonNull final PrintStream out) -> {
                out.println("__________             .__ ____  __.                .___.__ ");
                out.println("\\______   \\_____ ___  _|__|    |/ _|____   ____   __| _/|__|");
                out.println(" |       _/\\__  \\\\  \\/ /  |      < /  _ \\ /  _ \\ / __ | |  |");
                out.println(" |    |   \\ / __ \\\\   /|  |    |  (  <_> |  <_> ) /_/ | |  |");
                out.println(" |____|_  /(____  /\\_/ |__|____|__ \\____/ \\____/\\____ | |__|");
                out.println("        \\/      \\/                \\/                 \\/     ");
              })
              .run(args);
	}
}
