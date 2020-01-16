package nl.matsv.viabackwards.protocol.protocol1_12_2to1_13;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.data.BackwardsMappings;

// Slightly changed methods of the ChatRewriter
public class TranslationRewriter {

    public static String processTranslate(String value) {
        BaseComponent[] components = ComponentSerializer.parse(value);
        for (BaseComponent component : components) {
            processTranslate(component);
        }
        return components.length == 1 ? ComponentSerializer.toString(components[0]) : ComponentSerializer.toString(components);
    }

    private static void processTranslate(BaseComponent component) {
        if (component == null) return;
        if (component instanceof TranslatableComponent) {
            TranslatableComponent translatableComponent = (TranslatableComponent) component;
            String oldTranslate = translatableComponent.getTranslate();
            String newTranslate = BackwardsMappings.translateMappings.get(oldTranslate);
            if (newTranslate != null) {
                translatableComponent.setTranslate(newTranslate);
            }
            if (translatableComponent.getWith() != null) {
                for (BaseComponent baseComponent : translatableComponent.getWith()) {
                    processTranslate(baseComponent);
                }
            }
        }
        if (component.getHoverEvent() != null) {
            for (BaseComponent baseComponent : component.getHoverEvent().getValue()) {
                processTranslate(baseComponent);
            }
        }
        if (component.getExtra() != null) {
            for (BaseComponent baseComponent : component.getExtra()) {
                processTranslate(baseComponent);
            }
        }
    }
}
