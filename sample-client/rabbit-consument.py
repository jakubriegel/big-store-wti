import pika

connection = pika.BlockingConnection(pika.ConnectionParameters('localhost'))
channel = connection.channel()


def consume():
    channel.exchange_declare(exchange='user_profile', exchange_type='fanout')

    result = channel.queue_declare(queue='', exclusive=True)
    queue_name = result.method.queue

    channel.queue_bind(exchange='user_profile', queue=queue_name)

    def call(ch, method, properties, body):
        print(body)

    channel.basic_consume(queue=queue_name,
                          auto_ack=True,
                          on_message_callback=call)
    channel.start_consuming()


if __name__ == '__main__':
    consume()
