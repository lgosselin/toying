Micro benchmark project to investigate batch insertion with MySQL and Hibernate.

In particular some configuration elements are investigated:
* Impact of MySQL "rewriteBatchedStatements=true" parameter presence
* Impact of hibernate property <property name="hibernate.jdbc.batch_size" value="xx" />
* Impact of key generation strategy (IDENTITY and TABLE (with pre-allocation size = 50) are used)
* Impact of INSERT IGNORE statement of insertion perf (only tested for TABLE key generation strategy)

You will have to modify the code to run adapt this project to your needs. Have a look at Main class
and change the constant annotated with "Adapt this". Have a look at src/main/resources/create_database.sql
to setup the database.

Quick conclusions:
* Entity with @GeneratedValue(strategy = GenerationType.IDENTITY) won't be inserted in batch (regardless of the settings)
* If hibernate.jdbc.batch_size is not specified, its value is 0, nothing is batched on Hibernate side
* MySQL rewriteBatchedStatements is not really making a difference unless Hibernate is effectively batching the insertions
* With 15% of duplicate entities, INSERT IGNORE statements is really not far from a simple INSERT
* Activate both Hibernate batching and rewriteBatchedStatements=true to notice a substantive improvement
* Stay away of IDENTITY key generation for heavy INSERT workload

Measurement samples
-------------------

Timing for 100 000 insertion on my laptop
|                            Runtime in ms |             Identity |             Table id |  Table id with dedup |
|            No batching, no rewrite batch |                25532 |                30749 |                31694 |
|            No batching, rewrite batch on |                23280 |                31864 |                34584 |
|        Batching per 10, no rewrite batch |                22421 |                26204 |                29220 |
|        Batching per 10, rewrite batch on |                24647 |                11377 |                13862 |
|        Batching per 50, no rewrite batch |                21476 |                25908 |                27280 |
|        Batching per 50, rewrite batch on |                24285 |                10006 |                11512 |

Timing for 100000 insertions (second run)
|                            Runtime in ms |             Identity |             Table id |  Table id with dedup |
|            No batching, no rewrite batch |                23760 |                30593 |                31731 |
|            No batching, rewrite batch on |                22674 |                31463 |                32336 |
|        Batching per 10, no rewrite batch |                20809 |                25973 |                28891 |
|        Batching per 10, rewrite batch on |                23036 |                11581 |                14892 |
|        Batching per 50, no rewrite batch |                21376 |                26550 |                28047 |
|        Batching per 50, rewrite batch on |                22399 |                 9756 |                10877 |

For a really small set of insertions (too small to really trust the timing)

Timing for 350 insertions on my laptop
|                            Runtime in ms |             Identity |             Table id |  Table id with dedup |
|            No batching, no rewrite batch |                  226 |                  182 |                  216 |
|            No batching, rewrite batch on |                  215 |                  172 |                  172 |
|        Batching per 10, no rewrite batch |                  191 |                  102 |                  104 |
|        Batching per 10, rewrite batch on |                  188 |                   68 |                   68 |
|        Batching per 50, no rewrite batch |                  143 |                  167 |                  104 |
|        Batching per 50, rewrite batch on |                  144 |                   39 |                   55 |

Timing for 350 insertions on my laptop (second run)
|                            Runtime in ms |             Identity |             Table id |  Table id with dedup |
|            No batching, no rewrite batch |                  229 |                  204 |                  174 |
|            No batching, rewrite batch on |                  241 |                  185 |                  151 |
|        Batching per 10, no rewrite batch |                  233 |                  294 |                  219 |
|        Batching per 10, rewrite batch on |                  139 |                   48 |                   72 |
|        Batching per 50, no rewrite batch |                  119 |                   87 |                   88 |
|        Batching per 50, rewrite batch on |                  120 |                   41 |                   54 |