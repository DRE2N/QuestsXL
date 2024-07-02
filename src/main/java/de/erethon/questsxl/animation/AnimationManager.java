package de.erethon.questsxl.animation;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.QuestsXL;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AnimationManager {

    List<QAnimation> animations = new ArrayList<>();
    List<QCutscene> cutscenes = new ArrayList<>();

    public AnimationManager(File folder) {
        load(folder);
    }

    public QAnimation getAnimation(String id) {
        for (QAnimation animation : animations) {
            if (animation.getId().equalsIgnoreCase(id)) {
                return animation;
            }
        }
        return null;
    }

    public QCutscene getCutscene(String id) {
        for (QCutscene cutscene : cutscenes) {
            if (cutscene.getId().equalsIgnoreCase(id)) {
                return cutscene;
            }
        }
        return null;
    }

    public List<QAnimation> getAnimations() {
        return animations;
    }

    public List<QCutscene> getCutscenes() {
        return cutscenes;
    }

    public void load(File folder) {
        for (File file : folder.listFiles()) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            String id = file.getName().replaceAll(".yml","");
            if (id.contains("cutscene_")) {
                String cutsceneID = id.replaceAll("cutscene_", "");
                QCutscene cutscene = new QCutscene(cutsceneID);
                cutscene.load(config);
                cutscenes.add(cutscene);
                continue;
            }
            QAnimation animation = new QAnimation(id);
            animation.load(config);
            animations.add(animation);
        }
        MessageUtil.log("Loaded " + animations.size() + " animations.");
        MessageUtil.log("Loaded " + cutscenes.size() + " cutscenes.");
    }

    public void save() {
        for (QAnimation animation : animations) {
            YamlConfiguration configuration = animation.save();
            try {
                configuration.save(new File(QuestsXL.ANIMATIONS + "/" + animation.getId() + ".yml"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        for (QCutscene cutscene : cutscenes) {
            YamlConfiguration configuration = cutscene.save();
            try {
                configuration.save(new File(QuestsXL.ANIMATIONS + "/cutscene_" + cutscene.getId() + ".yml"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
