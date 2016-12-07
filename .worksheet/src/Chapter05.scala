import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.rdd.RDD
import org.apache.spark.rdd.RDD.numericRDDToDoubleRDDFunctions

object Chapter05 {;import org.scalaide.worksheet.runtime.library.WorksheetSupport._; def main(args: Array[String])=$execute{;$skip(328); 
  println("Welcome to the Scala worksheet");$skip(77); 
    /**�û�����****/
    val rawData = sc.textFile("ml-100k/train_noheader.tsv");System.out.println("""rawData  : <error> = """ + $show(rawData ));$skip(56); 
    val records = rawData.map(line => line.split("\t"));System.out.println("""records  : <error> = """ + $show(records ));$skip(19); val res$0 = 
     records.first;System.out.println("""res0: <error> = """ + $show(res$0));$skip(990); 
     
     
     //�������ݸ�ʽ�����⣬������һЩ��������Ĺ������Ѷ����(��)ȥ������
    val data = records.map { r =>
      //�Ѷ����(��)ȥ��
      val trimmed = r.map { x =>
        //println("befor:" + x)
        val v = x.replaceAll("\"", "")
        // println("after:" + v)
        v
      }
      //println("r:" + r.toList + "\t size:" + r.size)
      //r��Array[String] = Array("http://www.bloomberg.com/news/2010-12-23/ibm-predicts-holographic-calls-air-breathing-batteries-by-2015.html", "4042", ...
      //����һ������Ϊ0��ʼ
      val label = trimmed(r.size - 1).toInt
      println("label:" + label)
      //����һ�������������������������������ֵ����,ȡ�Ӽ�set(1,4ΪԪ��λ��, ��0��ʼ),��λ��4��ʼ,������ĳ���
      //slice��ȡ�����е�25�е���������
      //���ݼ���ȱʧ����Ϊ?,ֱ����0�滻ȱʧ����
      val features = trimmed.slice(4, r.size - 1).map{
        d => if (d == "?"){
          println(d)
          0.0
         
        }else{
           d.toDouble
        }
      }
      //����ǩ����������ת��ΪLabeledPointΪʵ��,�����������洢��MLib��Vectors��
      LabeledPoint(label, Vectors.dense(features))
    };System.out.println("""data  : <error> = """ + $show(data ))}
}
