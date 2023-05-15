package org.rsinitsyn.quiz.component.сustom;


import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.server.StreamResource;

@Tag("audio")
public class AudioPlayer extends Component {
    private static final long serialVersionUID = 111222L;

    public AudioPlayer(StreamResource path) {
        this();
        setSource(path);
    }

    public AudioPlayer() {
        getElement().setAttribute("controls", true);
        getElement().setAttribute("autoplay", true);
    }

    public void setSource(StreamResource resource) {
        getElement().setAttribute("src", resource);
    }
}
