import org.kohsuke.args4j.Option;

/**
 * Created by user on 2017/5/28.
 */
public class CmdArgs {
    @Option(name="-modelPath",usage="Sets a modelPath")
    public static String modelPath;

    @Option(name="-inputPath",usage="Sets an inputPath")
    public static String inputPath;
}
