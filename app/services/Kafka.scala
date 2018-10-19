package services

import akka.kafka.ConsumerMessage.CommittableMessage
import akka.kafka.scaladsl.Consumer.Control
import javax.inject.{Inject, Singleton}
import akka.kafka.scaladsl.{Consumer, Producer}
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream.scaladsl.{Sink, Source}
import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecord}
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer}
import play.api.Configuration
import util.Constants

import scala.util.{Failure, Success, Try}

trait Kafka {
  def source(topic: String): Try[Source[CommittableMessage[String, String], Control]]
}

@Singleton
class KafkaImpl @Inject() (configuration: Configuration) extends Kafka {


  def consumerSettings: Try[ConsumerSettings[String, String]] = {
    val config = configuration.getOptional[Configuration]("akka.kafka.consumer").getOrElse(Configuration.empty)
    Try {
      ConsumerSettings(config.underlying, new StringDeserializer, new StringDeserializer)
        .withBootstrapServers(Constants.kafkaUrl).withGroupId("group1")
        .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

    }
  }


  def source(topic: String): Try[Source[CommittableMessage[String, String], Control]] = {
    val subscriptions = Subscriptions.topics(topic)
    consumerSettings.map(Consumer.committableSource(_, subscriptions))
  }

}
