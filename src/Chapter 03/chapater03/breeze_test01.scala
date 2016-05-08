package chapater03


import org.apache.log4j.{ Level, Logger }
import org.apache.spark.{ SparkConf, SparkContext }
import breeze.linalg._
import breeze.numerics._
import org.apache.spark.mllib.linalg.Vectors

object breeze_test01 {

  def main(args: Array[String]) {
    val conf = new SparkConf().setMaster("local[2]").setAppName("SparkHdfsLR")
    val sc = new SparkContext(conf)
    Logger.getRootLogger.setLevel(Level.WARN)

    // 3.1.1 Breeze ��������
    val m1 = DenseMatrix.zeros[Double](2, 3)
    val v1 = DenseVector.zeros[Double](3)
    val v2 = DenseVector.ones[Double](3)
    val v3 = DenseVector.fill(3) { 5.0 }
    val v4 = DenseVector.range(1, 10, 2)
    val m2 = DenseMatrix.eye[Double](3)
    //�ԽǾ���
    /**
     * 1.0,0.0,0.0
     * 0.0,2.0,0.0
     * 0.0,0.0,3.0
     */
    val v6 = diag(DenseVector(1.0, 2.0, 3.0))
    /**
     * ���д�������
     * 1.0 2.0
     * 3.0 4.0
     */
    val m3 = DenseMatrix((1.0, 2.0), (3.0, 4.0))
    //���д�������
    val v8 = DenseVector(1, 2, 3, 4)
    val v9 = DenseVector(1, 2, 3, 4).t //����ת��
    //�Ӻ�����������
    val v10 = DenseVector.tabulate(3) { i => 2 * i }
    val m4 = DenseMatrix.tabulate(3, 2) { case (i, j) => i + j }
    //�����鴴������
    val v11 = new DenseVector(Array(1, 2, 3, 4))
    //�����鴴������,2��3��
    val m5 = new DenseMatrix(2, 3, Array(11, 12, 13, 21, 22, 23))
    //0��1���������
    val v12 = DenseVector.rand(4)
    //2��3���������
    val m6 = DenseMatrix.rand(2, 3)

    // 3.1.2 Breeze Ԫ�ط��ʼ���������
    // Ԫ�ط���
    val a = DenseVector(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    a(0)//ָ��λ��
    a(1 to 4)//�����Ӽ�
    a(5 to 0 by -1)//����ָ������ȡ�Ӽ�
    a(1 to -1)//ָ����ʼλ������β
    a(-1)//���һ��Ԫ��
    //��������2��3��
    val m = DenseMatrix((1.0, 2.0, 3.0), (3.0, 4.0, 5.0))
    m(0, 1)//ȡ��1��,2�����ݼ�2
    m(::, 1)//����ָ����,��ȡ2������,��2,4

    // ��������2��3��
    val m_1 = DenseMatrix((1.0, 2.0, 3.0), (3.0, 4.0, 5.0))
    /**
     * 1.0 4.0
     * 3.0 3.0
     * 2.0 5.0
     */
    m_1.reshape(3, 2)//��������3��2��
    /**
     * 1.0,3.0,2.0,4.0,3.0,5.0
     */
    m_1.toDenseVector//�Ѿ�������ʽת��������
     // ��������3��3��
    val m_3 = DenseMatrix((1.0, 2.0, 3.0), (4.0, 5.0, 6.0), (7.0, 8.0, 9.0))
    //����������
    lowerTriangular(m_3)
    //����������
    upperTriangular(m_3)
    //������
    m_3.copy
    //ȡ������Ԫ��
    /**
     * 1.0,20,3.0
     * 4.0,5.0,6.0
     * 7.0,8.0,9.0
     */
    diag(m_3)//ȡ������Ԫ�� 1.0,5.0,9.0
    //����3�и�ֵ5.0
    m_3(::, 2) := 5.0
    
    m_3
     //����ֵ(2��,2�����ʼ��ֵ5.0)
    m_3(1 to 2, 1 to 2) := 5.0
    m_3
    //��������
    val a_1 = DenseVector(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    //����������ֵ
    a_1(1 to 4) := 5
    a_1(1 to 4) := DenseVector(1, 2, 3, 4)
    a_1
    val a1 = DenseMatrix((1.0, 2.0, 3.0), (4.0, 5.0, 6.0))
    val a2 = DenseMatrix((1.0, 1.0, 1.0), (2.0, 2.0, 2.0))
    //��ֱ���Ӿ���,��������������,�в���,������
    DenseMatrix.vertcat(a1, a2)
    //�������Ӿ���,�в���,������
    DenseMatrix.horzcat(a1, a2)
    val b1 = DenseVector(1, 2, 3, 4)
    val b2 = DenseVector(1, 1, 1, 1)
    //��ֱ���Ӿ���,��������������,�в���,������
    DenseVector.vertcat(b1, b2)

    // 3.1.3 Breeze ��ֵ���㺯��
    val a_3 = DenseMatrix((1.0, 2.0, 3.0), (4.0, 5.0, 6.0))
    val b_3 = DenseMatrix((1.0, 1.0, 1.0), (2.0, 2.0, 2.0))
    a_3 + b_3 //��������Ԫ��ֵ���
    a_3 :* b_3//��������Ԫ��ֵ���
    a_3 :/ b_3//��������Ԫ��ֵ���
    a_3 :< b_3//��������Ԫ��ֵ�Ƚ�
    a_3 :== b_3//��������Ԫ��ֵ���
    a_3 :+= 1.0//Ԫ��ֵ׷��
    a_3 :*= 2.0//Ԫ��ֵ׷��
    max(a_3)//����Ԫ�����ֵ
    argmax(a_3)//����Ԫ�����ֵ��λ������
    DenseVector(1, 2, 3, 4) dot DenseVector(1, 1, 1, 1)

    // 3.1.4 Breeze ��ͺ���
    val a_4 = DenseMatrix((1.0, 2.0, 3.0), (4.0, 5.0, 6.0), (7.0, 8.0, 9.0))
    sum(a_4)//�Ծ������
    sum(a_4, Axis._0)//�Ծ���ÿ�����
    sum(a_4, Axis._1)//�Ծ���ÿ�����
    trace(a_4)//�Խ���Ԫ�����
    accumulate(DenseVector(1, 2, 3, 4))//�ۼƺ�,1,3,6,10,����һλ��ʼ����ۼ����
    

    // 3.1.5 Breeze ��������
    val a_5 = DenseVector(true, false, true)
    val b_5 = DenseVector(false, true, true)
    a_5 :& b_5 //Ԫ����
    a_5 :| b_5//Ԫ�ػ�
    !a_5//Ԫ�ط�
    val a_5_2 = DenseVector(1.0, 0.0, -2.0)
    any(a_5_2)//����Ԫ�ط�0
    all(a_5_2)//����Ԫ�ط�0

    // 3.1.6 Breeze ���Դ�������
    val a_6 = DenseMatrix((1.0, 2.0, 3.0), (4.0, 5.0, 6.0), (7.0, 8.0, 9.0))
    val b_6 = DenseMatrix((1.0, 1.0, 1.0), (1.0, 1.0, 1.0), (1.0, 1.0, 1.0))
    a_6 \ b_6//�������
    a_6.t//ת�� 
    det(a_6)//������ֵ
    inv(a_6)//����
    val svd.SVD(u, s, v) = svd(a_6)
    a_6.rows//��������
    a_6.cols//�������� 

    // 3.1.7 Breeze ȡ������
    //��������
    val a_7 = DenseVector(1.2, 0.6, -2.3)
    round(a_7)//����Ԫ����������
    ceil(a_7)//����Ԫ����С����
    floor(a_7)//����Ԫ���������
    signum(a_7)//����Ԫ�ط��ź���
    abs(a_7)//����Ԫ��ȡ���� 

  }
}