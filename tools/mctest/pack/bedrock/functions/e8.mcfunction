# [E8] 不写 location / 只在副手 | 期望匹配
scoreboard objectives add tt dummy
clear @s
replaceitem entity @s slot.weapon.offhand 0 diamond 1
execute if entity @s[hasitem={item=diamond,location=slot.weapon.offhand}] run scoreboard players set #e8s tt 1
execute unless entity @s[hasitem={item=diamond,location=slot.weapon.offhand}] run scoreboard players set #e8s tt 0
execute if entity @s[hasitem={item=diamond}] run scoreboard players set #e8 tt 1
execute unless entity @s[hasitem={item=diamond}] run scoreboard players set #e8 tt 0
