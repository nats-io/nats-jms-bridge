# 0.24.0-beta21 NATS JMS/MQ Bridge

#### TAG: 0.24.0-beta21

## Issues

* #254 Headers not getting copied
* #266 Cascade testing failures for IBM MQ unit/integration tests (fixed)
* Fixed Issue: forward mode correlation ID is not being passed from JMS all the way to NATS #256
* Fixed Issue: Forward mode correlation ID is not being passed from NATS all the way to JMS #255
* Correlation ID was not copied on purpose due to initial focus of specification on request/reply bridge
* Don't copy over correlation ID if passed for request/reply #268
