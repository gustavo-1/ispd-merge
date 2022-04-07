package ispd.fxgui.workload.dag.shapes;

import ispd.fxgui.commons.EdgeShape;
import ispd.fxgui.commons.PointedEdgeShape;
import javafx.scene.shape.Line;

public class PrefixShape extends EdgeShape {

    public PrefixShape() {
        super(new PointedEdgeShape(new Line() {
            {this.getStrokeDashArray().setAll(8.0, 5.0);}
        }));
    }
}
