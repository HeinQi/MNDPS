package com.rsi.mengniu;


	import java.awt.image.BufferedImage;  
	import java.awt.image.Raster;  
	import java.awt.image.WritableRaster;  
	import java.io.File;  
	import java.io.IOException;  
	import javax.imageio.ImageIO;  
	   
	public class Test { 
	    public void releaseSound(String filepath,String destpath, int Threshold){  
	        //过滤背景色进行黑白二值化处理  
	        try {  
	            BufferedImage bi=ImageIO.read(new File(filepath));  
	            int width=bi.getWidth();  
	            int height=bi.getHeight();  
	            BufferedImage bi2=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);  
	            Raster raster=bi.getRaster();  
	            WritableRaster wr=bi2.getRaster();  
	            for(int i=0;i<width;i++){  
	                for(int j=0;j<height;j++){  
	                    int[] a=new int[4];  
	                    raster.getPixel(i, j, a);  
	                    //System.out.println("("+a[0]+", "+a[1]+", "+a[2]+", "+a[3]+")");  
	                    if((a[0]+a[1]+a[2])/3>Threshold){  
	                        a[0]=255;  
	                        a[1]=255;  
	                        a[2]=255;  
	                        a[3]=255;  
	                        wr.setPixel(i, j, a);  
	                    }else{  
	                        a[0]=0;  
	                        a[1]=0;  
	                        a[2]=0;  
	                        a[3]=255;  
	                        wr.setPixel(i, j, a);  
	   
	                    }  
	                }  
	            }  
	            ImageIO.write(bi2, "PNG", new File(destpath));  
	        } catch (IOException e) {  
	            e.printStackTrace();  
	        }  
	    }  
	    
	    public static void binaryImage() throws IOException{  
	        File file = new File("/Users/haibin/Documents/temp/123.jpg");  
	        //System.out.println(file);
	        BufferedImage image = ImageIO.read(file);  
	          
	        int width = image.getWidth();  
	        int height = image.getHeight();  
	          
	        BufferedImage grayImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);  
	        for(int i= 0 ; i < width ; i++){  
	            for(int j = 0 ; j < height; j++){  
	            int rgb = image.getRGB(i, j);  
	            grayImage.setRGB(i, j, rgb);  
	            }  
	        }  
	          
	        File newFile = new File("/Users/haibin/Documents/temp/444.png");  
	        
	        ImageIO.write(grayImage, "jpg", newFile);  
	        }  
	    public static void main(String[] args) {  
	        new Test().releaseSound("/Users/haibin/Documents/temp/123.jpg","/Users/haibin/Documents/temp/333.png", 100);  
	        try {
				binaryImage();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }  
	}  

