package br.com.pointel.goorv.service.wizard;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import br.com.pointel.goorv.Activity;
import br.com.pointel.goorv.Glyphing;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;

public class WizAct {

    public static final String NAME = "goorv";

    public static final String ACTIVITY_PACKAGE = "br.com.pointel.goorv.activity";

    public static List<String> getActivities() {
        try (ScanResult scanResult = new ClassGraph().acceptPackages(ACTIVITY_PACKAGE)
                        .scan()) {
            return scanResult.getAllClasses().stream().map(c -> c.getName()).collect(
                            Collectors.toList());
        }
    }

    public static void execute(Consumer<Glyphing> consumer, String prompt) throws Exception {
        var allPrompt = WizChars.parseStrings(prompt);
        var pipedPrompt = new ArrayList<List<String>>();
        var currentPrompt = new ArrayList<String>();
        for (String promptPart : allPrompt) {
            if (promptPart.equals("|")) {
                pipedPrompt.add(currentPrompt);
                currentPrompt = new ArrayList<String>();
            } else {
                currentPrompt.add(promptPart);
            }
        }
        pipedPrompt.add(currentPrompt);
        var stackGlyph = new ArrayList<String>();
        var stackConsumer = (Consumer<Glyphing>) glyphing -> {
            if (glyphing.hasGlyphed()) {
                stackGlyph.add(glyphing.getGlyphed());
            } else if (glyphing.hasError()) {
                consumer.accept(glyphing);
            }
        };
        for (int i = pipedPrompt.size() -1; i >= 0; i--) {
            var currentConsumer = i > 0 ? stackConsumer : consumer;
            var promptCall = pipedPrompt.get(i);
            if (promptCall.isEmpty()) {
                continue;
            }
            var activityName = promptCall.remove(0);
            var activity = getActivity(currentConsumer, activityName);
            var orderName = promptCall.remove(0);
            var order = activity.getOrderOrThrow(orderName);
            promptCall.addAll(stackGlyph);
            stackGlyph.clear();
            if (promptCall.isEmpty()) {
                activity.call(order);
            } else {
                var params = order.getPassedBy(promptCall);
                activity.call(order, params);
            }
        }
    }

    public static Activity getActivity(Consumer<Glyphing> consumer, String activityName) throws Exception {
        Class<?> clazz = Class.forName(ACTIVITY_PACKAGE + "." + activityName);
        return (Activity) clazz.getConstructor(Consumer.class).newInstance(consumer);
    }

}
