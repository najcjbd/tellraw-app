# [R5] 单槽位 !0（已测：不命中） | 只看实测值
scoreboard objectives add tt dummy
clear @s
replaceitem entity @s slot.armor.head 0 diamond_helmet 1
execute if entity @s[hasitem={item=diamond_helmet,location=slot.armor.head}] run scoreboard players set #r5s tt 1
execute unless entity @s[hasitem={item=diamond_helmet,location=slot.armor.head}] run scoreboard players set #r5s tt 0
execute if entity @s[hasitem={item=diamond_helmet,location=slot.armor.head,slot=!0}] run scoreboard players set #r5 tt 1
execute unless entity @s[hasitem={item=diamond_helmet,location=slot.armor.head,slot=!0}] run scoreboard players set #r5 tt 0
