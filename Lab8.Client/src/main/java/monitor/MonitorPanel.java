package monitor;

import app.Application;
import collections.Vehicle;
import frame.MainFrame;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.TreeSet;
import javax.swing.JPanel;
import javax.swing.Timer;

public class MonitorPanel extends JPanel implements MouseListener, MouseMotionListener {

    class VehicleEntity {
        
        Vehicle vehicle;
        Shape shape;
        boolean hover;
        boolean selected;
        double r;
        Color color;
        float hue;
        float saturation;
        float dsaturation;
        
        VehicleEntity(Vehicle vehicle, Shape shape, Color color, double r) {
            this.vehicle = vehicle;
            this.shape = shape;
            this.color = color;
            this.r = r;
        }

        public boolean isHover() {
            return hover;
        }

        public void setHover(boolean hover) {
            this.hover = hover;
        }

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }
        
    }

    /** Scale constants */
    public static final float CAPACITY_KOEF = 1f;
    public static final float MIN_SIZE = 5.0f;
    
    /** Color constants */
    private final float SELECTED_HUE = 0.0f;
    private final float HOVER_SATURATION = 0.5f;
    private final float LUMINANCE = 0.9f;
    private final float MIN_SATURATION = 0.1f;
    private final float MAX_SATURATION = 1f;
    private final float DELTA_SATURATION = 0.05f;
    
    /** Animation constants */
    private final int FPS = 60;

    /** Parent frame */
    MainFrame parent;
    
    /** Scale settings for current viewport */
    private Point2D minPoint;
    private double screenK;
    private int screenH;
    private int screenW;
    
    
    /** HashMap: owner - color */
    private HashMap<String, Color> colors;
    
    /** Vehicle entities (data+visualization) */
    private final ArrayList<VehicleEntity> entities;
    
    
    
    private boolean ready = false;
    
    
    public MonitorPanel() {
        this.setBackground(Color.WHITE);
        addMouseListener(this);
        addMouseMotionListener(this);
        entities = new ArrayList<>();
        colors = new HashMap<>();
        startAnimation();
    }
    
    public void init(MainFrame parent) {
        this.parent = parent;
    }
    
    private void startAnimation() {
        Timer animator = new Timer(1000/FPS, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                repaint();
            }
        });
        animator.start();
    }
    
    @Override
    protected void paintComponent(Graphics grphcs) {
        super.paintComponent(grphcs);
        
        Graphics2D g2d = (Graphics2D) grphcs;
        
        if (ready) {
            g2d.drawLine(5, (int)Math.round(getScreenY(0)), screenW+5, (int)Math.round(getScreenY(0)));
            g2d.drawLine((int)Math.round(getScreenX(0)), 5, (int)Math.round(getScreenX(0)), screenH+5);

            Color color;
            VehicleEntity selectedEntity = null;
            for (VehicleEntity entity : entities) {
                
                if (entity.isSelected()) {
                    selectedEntity = entity;
                } else if (entity.isHover()) {
                    entity.hue = entity.hue + 0.01f;
                    color = Color.getHSBColor(entity.hue, HOVER_SATURATION, LUMINANCE);   
                    g2d.setPaint(color);
                } else {
                    g2d.setPaint(entity.color);
                }
                g2d.fill(entity.shape);                
                g2d.setColor(Color.BLACK);
                g2d.draw(entity.shape);
            }
            if (selectedEntity != null) {
                selectedEntity.saturation = selectedEntity.saturation + selectedEntity.dsaturation;
                if (selectedEntity.saturation > MAX_SATURATION) {
                    selectedEntity.saturation = MAX_SATURATION;
                    selectedEntity.dsaturation = -selectedEntity.dsaturation;
                } else if (selectedEntity.saturation < MIN_SATURATION) {
                    selectedEntity.saturation = MIN_SATURATION;
                    selectedEntity.dsaturation = -selectedEntity.dsaturation;
                }
                //System.out.println("entity.saturation: " + selectedEntity.saturation);
                color = Color.getHSBColor(SELECTED_HUE, selectedEntity.saturation, LUMINANCE);   
                g2d.setPaint(color);
                g2d.fill(selectedEntity.shape);                
                g2d.setColor(Color.BLACK);
                g2d.draw(selectedEntity.shape);
            }
            
            
        }
    }    
    
    public void setSelected(int selected) {
        entities.forEach((entity) -> {
            if (entity.vehicle.getId() == selected) {
                entity.setSelected(true);
                entity.saturation = MIN_SATURATION;
                entity.dsaturation = DELTA_SATURATION;
            } else {
                entity.setSelected(false);
            }
        });
    }


    @Override
    public void mouseClicked(MouseEvent e) {
        for (int i = 0; i < entities.size(); i++) {
            VehicleEntity entity = entities.get(i);
            if (entity.shape.contains(e.getPoint())) {
                parent.selectRow(entity.vehicle.getId());
            }
        }
    }
    
    @Override
    public void mouseMoved(MouseEvent e) {
        for (VehicleEntity entity : entities) {
            if (entity.shape.contains(e.getPoint())) {
                entity.hover = true;
            } else if (entity.hover) {
                entity.hover = false;
            }
        }
    }   

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }    
    
    @Override
    public void mouseDragged(MouseEvent e) {
    }    

    private void processVehicle(Vehicle vehicle) {
        Color color = colors.get(vehicle.getOwner());
        if (color == null) {
            color = getRandomColor();
            colors.put(vehicle.getOwner(), color);
        }
        
        double r = Math.sqrt(vehicle.getCapacity()/Math.PI);
        double size = CAPACITY_KOEF*r*screenK;
        if (size < 5) {
            size = 5;
        }
        
        Shape shape = new Ellipse2D.Double(getScreenX(vehicle.getCoordinates().getX())-size, getScreenY(vehicle.getCoordinates().getY())-size, 2*size, 2*size);

        VehicleEntity vehicleShape = new VehicleEntity(vehicle, shape, color, r);
        entities.add(vehicleShape);
    }
    
    private double getScreenX(double realX) {
        return 5 + (realX-minPoint.getX())*screenK;
    }
    
    private double getScreenY(double realY) {
        return screenH + 5 - (realY-minPoint.getY())*screenK;
    }
    
    private static Color getRandomColor() {
        Random random = new Random();
        float hue = random.nextFloat();
        // Saturation between 0.1 and 0.3
        float saturation = (random.nextInt(2000) + 1000) / 10000f;
        float luminance = 0.9f;
        Color color = Color.getHSBColor(hue, saturation, luminance);    

        /**
        Random random = new Random();
        float r = random.nextFloat();
        float g = random.nextFloat();
        float b = random.nextFloat();        
        Color color = new Color(r, g, b);
        */
        
        /**
        // Will produce only bright / light colours:
        Random random = new Random();
        float r = (float) (random.nextFloat()/2f + 0.5);
        float g = (float) (random.nextFloat() / 2f + 0.5);
        float b = (float) (random.nextFloat() / 2f + 0.5);      
        Color color = new Color(r, g, b);
        */
        
        return color;
    }
    
    public void setCollection(TreeSet<Vehicle> collection) {
        
        entities.clear();
        
        boolean first = true;

        double minX = 0;
        double maxX = 0;
        double minY = 0;
        double maxY = 0;
        
        for(Vehicle vehicle: collection) {
            double r = Math.sqrt(vehicle.getCapacity()/Math.PI);
            if (first) {
                minX = vehicle.getCoordinates().getX()-CAPACITY_KOEF*r;
                maxX = vehicle.getCoordinates().getX()+CAPACITY_KOEF*r;
                minY = vehicle.getCoordinates().getY()-CAPACITY_KOEF*r;
                maxY = vehicle.getCoordinates().getY()+CAPACITY_KOEF*r;
                //maxSize = vehicle.getCapacity();
                first = false;
            } else {
                if (vehicle.getCoordinates().getX()-CAPACITY_KOEF*r < minX) {
                    minX = vehicle.getCoordinates().getX()-CAPACITY_KOEF*r;
                }
                if (vehicle.getCoordinates().getX()+CAPACITY_KOEF*r > maxX) {
                    maxX = vehicle.getCoordinates().getX()+CAPACITY_KOEF*r;
                }
                if (vehicle.getCoordinates().getY()-CAPACITY_KOEF*r < minY) {
                    minY = vehicle.getCoordinates().getY()-CAPACITY_KOEF*r;
                }
                if (vehicle.getCoordinates().getY()+CAPACITY_KOEF*r > maxY) {
                    maxY = vehicle.getCoordinates().getY()+CAPACITY_KOEF*r;
                }
                /**
                if (vehicle.getCapacity() > maxSize) {
                    maxSize = vehicle.getCapacity();
                }
                */
            }
        }
        
        
        //System.out.println("["+minX+";"+minY+"]");
        //System.out.println("["+maxX+";"+maxY+"]");

        screenH = this.getSize().height - 10;
        screenW = this.getSize().width - 10;
        double kx = (this.getSize().width-10)/(maxX-minX);
        double ky = (this.getSize().height-10)/(maxY-minY);
        
        //System.out.println("kx: " + kx);
        //System.out.println("ky: " + ky);
        
        screenK = kx>ky?ky:kx;
        
        //System.out.println("screenK: " + screenK);
        
        minPoint = new Point.Double(
                (minX+(maxX-minX)/2-(this.getSize().width-10)/screenK/2), 
                (minY+(maxY-minY)/2-(this.getSize().height-10)/screenK/2)
        ); 
        
        //System.out.println("minPoint: " + minPoint);
        
        //System.out.println("["+getScreenX(0)+";"+getScreenY(0)+"]");
        
        for(Vehicle vehicle: collection) {
            processVehicle(vehicle);
        }
        
        ready = true;
        
        repaint();
    }
    
    public void checkHole() {
        for (int i=0; i<entities.size(); i++) {
            for (int j=i+1; j<entities.size(); j++) {
                VehicleEntity iEntity = entities.get(i);
                VehicleEntity jEntity = entities.get(j);
                if (Application.getInstance().getUser().getLogin().equals(iEntity.vehicle.getOwner()) && iEntity.vehicle.getOwner().equals(jEntity.vehicle.getOwner())) {
                    float deltax = iEntity.vehicle.getCoordinates().getX()-jEntity.vehicle.getCoordinates().getX();
                    float deltay = iEntity.vehicle.getCoordinates().getY()-jEntity.vehicle.getCoordinates().getY();
                    double distance = Math.sqrt(deltax*deltax + deltay*deltay);
                    if (distance < (iEntity.r + jEntity.r)) {
                        Application.getInstance().eatVehicle(iEntity.vehicle.getId(), jEntity.vehicle.getId());
                    }
                }
            }
        }
    }

    
}