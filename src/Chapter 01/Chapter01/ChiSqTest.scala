package Chapter01

import org.apache.spark.mllib.stat.test.ChiSqTestResult
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.stat.Statistics

object ChiSqTest {
  def main(args: Array[String]): Unit = {
     val vec1 = Vectors.dense(1, 7, 2, 3, 18);
     val vec2 = Vectors.dense(7, 8, 6, 7, 9);
     val goodnessOfFitTestResult1 = Statistics.chiSqTest(vec1);
     val goodnessOfFitTestResult2 = Statistics.chiSqTest(vec2);
    System.out.println(goodnessOfFitTestResult1);
    System.out.println(goodnessOfFitTestResult2);
    
    /**
     * �������:
     * 1)����1.0,7.0,2.0,3.0,18.0�� Ei=6.2(1.0,7.0,2.0,3.0,18.0) ��ӳ���/(������)5
     * 2)
     * Chi squared test summary:
      method: pearson
      degrees of freedom = 4 (���ɶ�)
      statistic = 31.41935483870968(���㿨���춨��ͳ��ֵ)
      //����
      pValue = 2.513864414077638E-6 
      Very strong presumption against null hypothesis: observed follows the same distribution as expected..
      Chi squared test summary:
      method: pearson
      degrees of freedom = 4 
      statistic = 0.7027027027027026 
      pValue = 0.9509952049458091 
      No presumption against null hypothesis: observed follows the same distribution as expected..
     * 
     */
  }
}